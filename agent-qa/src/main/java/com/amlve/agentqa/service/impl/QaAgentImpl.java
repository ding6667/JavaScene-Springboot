package com.amlve.agentqa.service.impl;

import com.amlve.agentqa.dto.context.ContextMessage;
import com.amlve.agentqa.mapper.QaRecordMapper;
import com.amlve.agentqa.service.QaContextManager;
import com.amlve.agentqa.tableEnt.QaRecord;
import com.amlve.agentqaapi.dto.request.QARequest;
import com.amlve.agentqaapi.dto.request.QaHistoryQueryRequest;
import com.amlve.agentqaapi.dto.response.QAResponse;
import com.amlve.agentqaapi.dto.response.QaHistoryVO;
import com.amlve.agentqaapi.dto.response.QaWorkflowAnswerVO;
import com.amlve.agentqaapi.service.QAAgentClient;
import com.amlve.agentqaapi.service.QaAgent;
import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.knowledgeapi.dto.request.KnowledgeVerifyRequest;
import com.amlve.knowledgeapi.dto.response.KnowledgePointDTO;
import com.amlve.knowledgeapi.dto.response.KnowledgeVerifyResponse;
import com.amlve.knowledgeapi.service.KnowledgeChapterApi;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 答疑智能体实现：多轮上下文（Redis）+ 问答记录（MySQL）双层存储
 * <p>
 * 一轮问答分成两段，供同步与流式两条入口共用（对应任务书 6.18.6，避免两套落库逻辑走偏）：
 * <ul>
 *     <li>{@link #beginTurn} —— 定会话、取轮次、读上下文、拼工作流入参；</li>
 *     <li>{@link #finishTurn} —— 校验答案非空、按知识库口径回填引用与超纲标志、
 *         追加上下文、落库，返回给学生。</li>
 * </ul>
 * 落库时机是<b>拿到完整答案之后</b>：流式中途断开时 finishTurn 根本不会被调用，
 * 因此不会留下半截答案的记录。
 * <p>
 * 三处"宁缺不假"的口径（都是九月"接口成功但内容为空"那类问题的同类）：
 * <ul>
 *     <li>答案为空：不落库，直接报答疑失败（answer 列 NOT NULL，塞空串就是假记录）；</li>
 *     <li>教材校验判不了（该章没配关键词或原文）：is_textbook_match 落 NULL 而不是默认"贴合"；</li>
 *     <li>Dify 没回 token / 执行ID：对应列落 NULL，同时记 warn。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QaAgentImpl implements QaAgent {

    /**
     * 一轮问答的过程态，从 beginTurn 传到 finishTurn。
     * <p>
     * 做成不可变记录而不是成员变量：智能体是多例共享的 Bean，把"本轮"存进字段会串到别的学生的请求上。
     *
     * @param userId    提问学生ID（请求线程内从令牌取出，流式回调线程拿不到 SecurityContext）
     * @param chapterId 章节ID，可为 null
     * @param sessionId 会话ID（本轮定稿后的，可能由后端新生成）
     * @param roundNo   本轮轮次
     * @param question  学生原始问题（落库存这个，不存拼了历史的那一大段）
     * @param prompt    发给工作流的入参（原始问题 + 历史上下文）
     * @param startTime 本轮开始时间戳（毫秒），用于端到端耗时
     */
    public record QaTurn(Long userId, Long chapterId, String sessionId, int roundNo,
                         String question, String prompt, long startTime) {
    }

    /**
     * knowledge_points 列宽 varchar(500)
     */
    private static final int KNOWLEDGE_POINTS_MAX_LENGTH = 500;

    /**
     * 历史问答单页上限，防止前端传个巨大的 pageSize 把整表问答拉走
     */
    private static final int MAX_HISTORY_PAGE_SIZE = 100;

    /**
     * 提示词里历史段的引导语
     */
    private static final String HISTORY_HEADER =
            "以下是本会话此前的对话，只用于理解指代（如\"它\"\"这一段\"），不要复述历史内容：";

    private final QaContextManager contextManager;

    private final QaRecordMapper qaRecordMapper;

    private final QAAgentClient qaAgentClient;

    /**
     * 教材校验口径来自 knowledge 模块（任务 4 的实现）：答疑是否超纲、引用哪段原文，
     * 与教师侧内容校验用同一把尺子，不另起一套判定
     */
    private final KnowledgeChapterApi knowledgeChapterApi;

    @Override
    public Result<QAResponse> answer(Long userId, QARequest request) {
        if (request == null || !StringUtils.hasText(request.getQuestion())) {
            throw new BusinessException("问题内容不能为空");
        }
        QaTurn turn = beginTurn(userId, request.getChapterId(), request.getSessionId(), request.getQuestion());
        Result<QaWorkflowAnswerVO> workflowResult =
                qaAgentClient.answer(chapterKeyOf(turn), turn.prompt());
        return Result.success(finishTurn(turn, unwrap(workflowResult)));
    }

    /**
     * 开始一轮问答：定会话ID → 取轮次 → 读上下文 → 拼工作流入参
     *
     * @param userId    学生ID（必须是请求线程里从令牌取到的）
     * @param chapterId 章节ID，可为 null
     * @param sessionId 前端回传的会话ID，为空表示首轮
     * @param question  学生原始问题
     * @return 本轮过程态
     */
    public QaTurn beginTurn(Long userId, Long chapterId, String sessionId, String question) {
        if (userId == null) {
            throw new BusinessException("提问用户ID缺失，无法记录问答");
        }
        if (!StringUtils.hasText(question)) {
            throw new BusinessException("问题内容不能为空");
        }
        String resolvedSessionId = StringUtils.hasText(sessionId)
                ? sessionId.trim() : UUID.randomUUID().toString().replace("-", "");
        int roundNo = contextManager.nextRoundNo(resolvedSessionId, maxRoundNoInDb(resolvedSessionId));
        List<ContextMessage> context = contextManager.getContext(userId, chapterId);
        String prompt = buildPrompt(context, question);
        log.info("答疑开始: userId={}, chapterId={}, sessionId={}, roundNo={}, 历史消息={}条",
                userId, chapterId, resolvedSessionId, roundNo, context.size());
        return new QaTurn(userId, chapterId, resolvedSessionId, roundNo, question, prompt,
                System.currentTimeMillis());
    }

    /**
     * 结束一轮问答：回填教材引用与超纲标志 → 追加上下文 → 落库 → 组装响应
     * <p>
     * 上下文与落库都只在拿到有效答案后执行；两者失败都不影响学生已经拿到的答案
     * （落库失败会记 error，学情统计缺一条比答疑整体失败轻）。
     *
     * @param turn   {@link #beginTurn} 返回的过程态
     * @param result Dify 工作流产出的答案与执行元数据
     * @return 面向学生的答疑结果（含 sessionId / roundNo 供前端续接会话）
     */
    public QAResponse finishTurn(QaTurn turn, QaWorkflowAnswerVO result) {
        String answer = result == null ? null : result.getAnswer();
        if (!StringUtils.hasText(answer)) {
            log.error("答疑未产出有效答案，不落库也不追加上下文: sessionId={}, roundNo={}",
                    turn.sessionId(), turn.roundNo());
            throw new BusinessException("答疑失败：智能体未返回有效答案，请重试或换一种问法");
        }
        int responseTime = (int) (System.currentTimeMillis() - turn.startTime());
        KnowledgeVerifyResponse verify = verifyAgainstTextbook(turn.chapterId(), answer);
        String knowledgePoints = matchKnowledgePoints(turn.chapterId(), answer);

        QAResponse response = new QAResponse();
        response.setAnswer(answer);
        response.setSessionId(turn.sessionId());
        response.setRoundNo(turn.roundNo());
        response.setKnowledgePoints(knowledgePoints);
        response.setReferences(verify == null || !StringUtils.hasText(verify.getStandardAnswer())
                ? Collections.emptyList() : List.of(verify.getStandardAnswer()));
        response.setHitRate(verify == null ? null : verify.getHitRate());
        response.setIsTextbookMatch(verify == null ? null : verify.getIsMatch());

        contextManager.appendRound(turn.userId(), turn.chapterId(), turn.question(), answer);
        saveRecord(turn, response, result, responseTime);
        log.info("答疑完成: sessionId={}, roundNo={}, 耗时={}ms, tokens={}, 是否贴合教材={}",
                turn.sessionId(), turn.roundNo(), responseTime,
                result.getTokensUsed(), response.getIsTextbookMatch());
        return response;
    }

    /**
     * 流式答疑收尾：把累积出的完整答案按同一套逻辑追加上下文并落库
     * <p>
     * 流式通道的 Dify 事件里也带 total_tokens，但 {@code answerStream} 的契约只透出文本片段
     * （拆分 think/正文由控制层负责），故流式的 tokens_used / dify_message_id 暂落 NULL 并记 warn，
     * 不写假值；同步 ask 通道才有完整的三列元数据。
     *
     * @param turn   {@link #beginTurn} 返回的过程态
     * @param answer 从流里累积出的完整答案（已剥思考链）
     * @return 本轮问答结果
     */
    public QAResponse finishStreamTurn(QaTurn turn, String answer) {
        return finishTurn(turn, new QaWorkflowAnswerVO(answer, null, null, null));
    }

    @Override
    public Result<PageResult<QaHistoryVO>> history(Long userId, QaHistoryQueryRequest request) {
        if (userId == null) {
            throw new BusinessException("用户未登录，无法查询问答历史");
        }
        QaHistoryQueryRequest query = request == null ? new QaHistoryQueryRequest() : request;
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1
                ? 10 : Math.min(query.getPageSize(), MAX_HISTORY_PAGE_SIZE);

        LambdaQueryWrapper<QaRecord> wrapper = Wrappers.<QaRecord>lambdaQuery()
                .eq(QaRecord::getUserId, userId)
                .eq(query.getChapterId() != null, QaRecord::getChapterId, query.getChapterId());
        boolean singleSession = StringUtils.hasText(query.getSessionId());
        if (singleSession) {
            // 查某一次完整对话时按轮次升序：时间戳同一毫秒会打乱"第一问/第二问"的顺序
            wrapper.eq(QaRecord::getSessionId, query.getSessionId().trim())
                    .orderByAsc(QaRecord::getRoundNo);
        } else {
            wrapper.orderByDesc(QaRecord::getCreateTime).orderByDesc(QaRecord::getId);
        }

        Page<QaRecord> pageData = qaRecordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<QaHistoryVO> records = pageData.getRecords().stream().map(this::toHistoryVO)
                .collect(Collectors.toList());
        PageResult<QaHistoryVO> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(pageData.getTotal());
        result.setCurrent(pageData.getCurrent());
        result.setSize(pageData.getSize());
        log.info("查询问答历史: userId={}, chapterId={}, sessionId={}, 命中={}条/共{}条",
                userId, query.getChapterId(), query.getSessionId(), records.size(), pageData.getTotal());
        return Result.success(result);
    }

    /**
     * 问库里该会话已有的最大轮次，只作为 Redis 计数归零时的水位参照。
     * <p>
     * 查询失败按 0 处理：拿不到水位也要让学生问得出去，宁可轮次重号（无唯一键，插得进）也不能报错。
     */
    private int maxRoundNoInDb(String sessionId) {
        try {
            Integer max = qaRecordMapper.selectMaxRoundNo(sessionId);
            return max == null ? 0 : max;
        } catch (Exception e) {
            log.warn("查询会话最大轮次失败，本轮次不做水位校正: sessionId={}, err={}", sessionId, e.getMessage());
            return 0;
        }
    }

    /**
     * 拼工作流入参：历史按"role: 内容"逐行列出，最后一行是当前问题。
     * <p>
     * 存进 qa_record 的仍是学生的原始问题，不是这一大段——否则历史会随每轮越滚越长，
     * 既没法读也没法统计。
     */
    private String buildPrompt(List<ContextMessage> context, String question) {
        if (context.isEmpty()) {
            return question;
        }
        StringBuilder prompt = new StringBuilder(HISTORY_HEADER);
        for (ContextMessage message : context) {
            prompt.append('\n').append(message.getRole()).append(": ").append(message.getContent());
        }
        prompt.append("\nuser: ").append(question);
        return prompt.toString();
    }

    /**
     * 章节ID在工作流入参里是字符串（非数字原样传，由 Dify 侧按未指定章节处理）
     */
    private String chapterKeyOf(QaTurn turn) {
        return turn.chapterId() == null ? null : String.valueOf(turn.chapterId());
    }

    /**
     * 用任务 4 的校验口径判定本轮回答是否贴合教材
     *
     * @return 校验结果；自由提问（无章节）或该章没配关键词/原文时返回 null 表示"判不了"
     */
    private KnowledgeVerifyResponse verifyAgainstTextbook(Long chapterId, String answer) {
        if (chapterId == null) {
            return null;
        }
        try {
            KnowledgeVerifyRequest request = new KnowledgeVerifyRequest();
            request.setChapterId(chapterId);
            request.setAiAnswer(answer);
            Result<KnowledgeVerifyResponse> result = knowledgeChapterApi.verify(request);
            if (result == null || result.getCode() != ErrorCodeConstant.SUCCESS) {
                log.warn("教材校验未通过，本轮不判是否超纲: chapterId={}, message={}", chapterId,
                        result == null ? "无响应" : result.getMessage());
                return null;
            }
            return result.getData();
        } catch (Exception e) {
            log.warn("教材校验不可用，本轮不判是否超纲: chapterId={}, err={}", chapterId, e.getMessage());
            return null;
        }
    }

    /**
     * 取该章知识点里被回答命中的名称（逗号分隔），供学情分析按知识点维度统计提问量
     * <p>
     * 只做包含匹配：这是"哪个知识点被问到"的粗粒度归因，不做语义推断，
     * 归因错了会污染学情，所以取不到就留空而不是猜。
     */
    private String matchKnowledgePoints(Long chapterId, String answer) {
        if (chapterId == null) {
            return null;
        }
        try {
            Result<List<KnowledgePointDTO>> result = knowledgeChapterApi.listPointsByChapter(chapterId);
            if (result == null || result.getCode() != ErrorCodeConstant.SUCCESS
                    || result.getData() == null || result.getData().isEmpty()) {
                return null;
            }
            String lowerAnswer = answer.toLowerCase(Locale.ROOT);
            List<String> matched = new ArrayList<>();
            int used = 0;
            for (KnowledgePointDTO point : result.getData()) {
                String name = point.getName();
                if (!StringUtils.hasText(name) || name.length() < 2) {
                    // 单字知识点（"类""流"）包含匹配几乎必然误命中，宁可不标
                    continue;
                }
                if (lowerAnswer.contains(name.toLowerCase(Locale.ROOT))) {
                    String candidate = name.length() > KNOWLEDGE_POINTS_MAX_LENGTH
                            ? name.substring(0, KNOWLEDGE_POINTS_MAX_LENGTH) : name;
                    if (used + candidate.length() + 1 > KNOWLEDGE_POINTS_MAX_LENGTH) {
                        break;
                    }
                    matched.add(candidate);
                    used += candidate.length() + 1;
                }
            }
            return matched.isEmpty() ? null : String.join(",", matched);
        } catch (Exception e) {
            log.warn("知识点归因失败，本轮不落 knowledge_points: chapterId={}, err={}",
                    chapterId, e.getMessage());
            return null;
        }
    }

    /**
     * 落库本轮问答。失败只记 error，不打断已经拿到答案的学生
     */
    private void saveRecord(QaTurn turn, QAResponse response, QaWorkflowAnswerVO result, int responseTime) {
        try {
            QaRecord record = new QaRecord();
            record.setUserId(turn.userId());
            record.setChapterId(turn.chapterId());
            record.setSessionId(turn.sessionId());
            record.setRoundNo(turn.roundNo());
            record.setQuestion(turn.question());
            record.setAnswer(response.getAnswer());
            record.setKnowledgePoints(response.getKnowledgePoints());
            record.setReferenceText(response.getReferences() == null || response.getReferences().isEmpty()
                    ? null : response.getReferences().get(0));
            record.setIsTextbookMatch(response.getIsTextbookMatch() == null
                    ? null : (Boolean.TRUE.equals(response.getIsTextbookMatch()) ? 1 : 0));
            record.setDifyMessageId(result.getDifyMessageId());
            record.setTokensUsed(result.getTokensUsed());
            record.setResponseTime(responseTime);
            qaRecordMapper.insert(record);
            if (record.getId() == null) {
                log.warn("问答记录未取到自增主键，中台上报如需 relationId 需按 session+round 反查: "
                        + "sessionId={}, roundNo={}", turn.sessionId(), turn.roundNo());
            }
        } catch (Exception e) {
            log.error("问答记录落库失败，本轮问答不会出现在历史记录与学情统计里: sessionId={}, roundNo={}",
                    turn.sessionId(), turn.roundNo(), e);
        }
    }

    private QaHistoryVO toHistoryVO(QaRecord record) {
        QaHistoryVO vo = new QaHistoryVO();
        vo.setId(record.getId());
        vo.setChapterId(record.getChapterId());
        vo.setSessionId(record.getSessionId());
        vo.setRoundNo(record.getRoundNo());
        vo.setQuestion(record.getQuestion());
        vo.setAnswer(record.getAnswer());
        vo.setKnowledgePoints(record.getKnowledgePoints());
        vo.setReferenceText(record.getReferenceText());
        vo.setIsTextbookMatch(record.getIsTextbookMatch());
        vo.setTokensUsed(record.getTokensUsed());
        vo.setResponseTime(record.getResponseTime());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    /**
     * 解包 Dify 调用结果：失败码必须转异常，直接 getData() 会拿到 null 再往下走成假成功
     */
    private QaWorkflowAnswerVO unwrap(Result<QaWorkflowAnswerVO> result) {
        if (result == null) {
            throw new BusinessException("答疑智能体无响应，请稍后重试");
        }
        if (result.getCode() != ErrorCodeConstant.SUCCESS) {
            throw new BusinessException("答疑失败: " + result.getMessage());
        }
        return result.getData();
    }
}
