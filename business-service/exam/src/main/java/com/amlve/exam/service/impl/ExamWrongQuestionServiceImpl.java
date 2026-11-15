package com.amlve.exam.service.impl;

import com.amlve.common.constant.ErrorMessageConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.exam.dto.response.WrongQuestionRetryVO;
import com.amlve.exam.mapper.ExamQuestionMapper;
import com.amlve.exam.mapper.ExamWrongQuestionMapper;
import com.amlve.exam.service.ExamDifficultyConfigService;
import com.amlve.exam.service.ExamQuestionSnapshotService;
import com.amlve.exam.service.ExamWrongQuestionService;
import com.amlve.exam.tableEnt.ExamQuestion;
import com.amlve.exam.tableEnt.ExamQuestionSnapshot;
import com.amlve.exam.tableEnt.ExamWrongQuestion;
import com.amlve.exam.util.QuestionAnswerCodec;
import com.amlve.exam.util.QuestionSnapshotCodec;
import com.amlve.examapi.dto.request.WrongQuestionArchiveRequest;
import com.amlve.examapi.dto.response.WrongQuestionRetryDTO;
import com.amlve.examapi.dto.response.WrongQuestionVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 错题归档服务实现。
 * <p>
 * 两张表分工：
 * <ul>
 *   <li>{@code exam_question_snapshot}：题目版本，一个版本一行，所有学生复用；</li>
 *   <li>{@code exam_wrong_question}：学生 × 快照 × 来源，只存计数字段，不存题面。</li>
 * </ul>
 * 展示、重做一律读快照，不实时 join 题库，因此题库改题不会让历史错题的题面漂移。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamWrongQuestionServiceImpl implements ExamWrongQuestionService {

    private static final int MASTERED = 1;
    private static final int NOT_MASTERED = 0;
    private static final int SOURCE_HOMEWORK = 1;

    /** 与 exam_homework_submit.answer_content 长度对齐，避免超长答案写库失败 */
    private static final int ANSWER_MAX_LENGTH = 2000;

    private final ExamWrongQuestionMapper wrongQuestionMapper;
    private final ExamQuestionMapper questionMapper;
    private final ExamQuestionSnapshotService snapshotService;
    private final ExamDifficultyConfigService difficultyConfigService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archive(Long userId, WrongQuestionArchiveRequest request) {
        if (userId == null || request == null || request.getQuestionId() == null) {
            return;
        }
        ExamQuestionSnapshot snapshot = resolveSnapshot(request);
        if (snapshot == null) {
            log.warn("错题归档跳过：题目快照无法生成, userId={}, questionId={}, snapshotId={}",
                    userId, request.getQuestionId(), request.getSnapshotId());
            return;
        }
        int source = request.getSource() == null ? SOURCE_HOMEWORK : request.getSource();
        String lastAnswer = truncate(request.getLastAnswer());
        LocalDateTime now = LocalDateTime.now();

        ExamWrongQuestion exist = wrongQuestionMapper.selectOne(Wrappers.<ExamWrongQuestion>lambdaQuery()
                .eq(ExamWrongQuestion::getUserId, userId)
                .eq(ExamWrongQuestion::getSnapshotId, snapshot.getId())
                .eq(ExamWrongQuestion::getSource, source));
        if (exist == null) {
            ExamWrongQuestion record = new ExamWrongQuestion();
            record.setUserId(userId);
            record.setSnapshotId(snapshot.getId());
            record.setSource(source);
            record.setWrongCount(1);
            record.setLastAnswer(lastAnswer);
            record.setMastered(NOT_MASTERED);
            record.setLastWrongTime(now);
            try {
                wrongQuestionMapper.insert(record);
            } catch (DuplicateKeyException e) {
                // 逻辑删除的行占着唯一键：复活并累计，不能再插一条
                wrongQuestionMapper.revive(userId, snapshot.getId(), source, lastAnswer, now);
            }
            return;
        }
        exist.setWrongCount((exist.getWrongCount() == null ? 0 : exist.getWrongCount()) + 1);
        exist.setLastAnswer(lastAnswer);
        exist.setMastered(NOT_MASTERED);
        exist.setLastWrongTime(now);
        wrongQuestionMapper.updateById(exist);
    }

    @Override
    public List<WrongQuestionVO> listWrongQuestions(Long userId) {
        List<ExamWrongQuestion> records = wrongQuestionMapper.selectList(Wrappers.<ExamWrongQuestion>lambdaQuery()
                .eq(ExamWrongQuestion::getUserId, userId)
                .orderByDesc(ExamWrongQuestion::getLastWrongTime));
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        Map<Long, ExamQuestionSnapshot> snapshotMap = snapshotService.mapByIds(records.stream()
                .map(ExamWrongQuestion::getSnapshotId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        Map<Long, ExamQuestion> latestQuestions = loadLatestQuestions(snapshotMap);
        // 难度名按快照记录的难度配置取，题库改了难度也不影响历史错题的展示
        Map<Long, String> difficultyNames = difficultyConfigService.namesByIds(snapshotMap.values().stream()
                .map(ExamQuestionSnapshot::getDifficultyConfigId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());

        List<WrongQuestionVO> result = new ArrayList<>();
        for (ExamWrongQuestion record : records) {
            ExamQuestionSnapshot snapshot = snapshotMap.get(record.getSnapshotId());
            if (snapshot == null) {
                log.warn("错题快照缺失，跳过该条: wrongQuestionId={}, snapshotId={}",
                        record.getId(), record.getSnapshotId());
                continue;
            }
            JsonNode view = snapshot.getSnapshotView();
            WrongQuestionVO vo = new WrongQuestionVO();
            vo.setId(record.getId());
            vo.setSnapshotId(snapshot.getId());
            vo.setSnapshotType(snapshot.getSnapshotType());
            vo.setQuestionId(snapshot.getQuestionId());
            vo.setContent(QuestionSnapshotCodec.text(view, "content"));
            vo.setShortTitle(snapshot.getShortTitle() != null
                    ? snapshot.getShortTitle() : QuestionSnapshotCodec.text(view, "shortTitle"));
            vo.setType(snapshot.getQuestionType() != null
                    ? snapshot.getQuestionType() : QuestionSnapshotCodec.integer(view, "type"));
            vo.setDifficultyConfigId(snapshot.getDifficultyConfigId());
            vo.setDifficultyName(difficultyNames.get(snapshot.getDifficultyConfigId()));
            vo.setSource(record.getSource());
            vo.setWrongCount(record.getWrongCount());
            vo.setLastAnswer(record.getLastAnswer());
            vo.setMastered(record.getMastered());
            vo.setLastWrongTime(record.getLastWrongTime());
            vo.setOutdated(isOutdated(snapshot, latestQuestions.get(snapshot.getQuestionId())));
            result.add(vo);
        }
        return result;
    }

    @Override
    public WrongQuestionRetryVO getRetryView(Long userId, Long wrongQuestionId) {
        ExamWrongQuestion record = requireOwned(userId, wrongQuestionId);
        ExamQuestionSnapshot snapshot = requireSnapshot(record);
        JsonNode view = snapshot.getSnapshotView();
        WrongQuestionRetryVO vo = new WrongQuestionRetryVO();
        vo.setWrongQuestionId(record.getId());
        vo.setSnapshotId(snapshot.getId());
        vo.setQuestionId(snapshot.getQuestionId());
        vo.setSource(record.getSource());
        vo.setSnapshotType(snapshot.getSnapshotType());
        vo.setContent(QuestionSnapshotCodec.text(view, "content"));
        vo.setShortTitle(snapshot.getShortTitle() != null
                ? snapshot.getShortTitle() : QuestionSnapshotCodec.text(view, "shortTitle"));
        vo.setType(snapshot.getQuestionType() != null
                ? snapshot.getQuestionType() : QuestionSnapshotCodec.integer(view, "type"));
        vo.setOptions(QuestionSnapshotCodec.stringList(view, "options"));
        vo.setRenderConfig(QuestionSnapshotCodec.node(view, "renderConfig"));
        vo.setCodeFramework(QuestionSnapshotCodec.text(view, "codeFramework"));
        vo.setRequirements(QuestionSnapshotCodec.stringList(view, "requirements"));
        vo.setHints(QuestionSnapshotCodec.stringList(view, "hints"));
        vo.setKnowledgePointIds(QuestionSnapshotCodec.longList(view, "knowledgePoints"));
        vo.setScore(snapshot.getScore() != null
                ? snapshot.getScore() : QuestionSnapshotCodec.integer(view, "score"));
        vo.setChapterId(snapshot.getChapterId() != null
                ? snapshot.getChapterId() : QuestionSnapshotCodec.longValue(view, "chapterId"));
        vo.setLastAnswer(record.getLastAnswer());
        vo.setWrongCount(record.getWrongCount());
        vo.setMastered(record.getMastered());
        vo.setLastWrongTime(record.getLastWrongTime());
        ExamQuestion latest = questionMapper.selectById(snapshot.getQuestionId());
        vo.setOutdated(isOutdated(snapshot, latest));
        return vo;
    }

    @Override
    public WrongQuestionRetryDTO getRetryPayload(Long userId, Long wrongQuestionId) {
        ExamWrongQuestion record = requireOwned(userId, wrongQuestionId);
        ExamQuestionSnapshot snapshot = requireSnapshot(record);
        JsonNode view = snapshot.getSnapshotView();
        JsonNode judge = snapshot.getSnapshotJudge();

        WrongQuestionRetryDTO dto = new WrongQuestionRetryDTO();
        dto.setWrongQuestionId(record.getId());
        dto.setSnapshotId(snapshot.getId());
        dto.setQuestionId(snapshot.getQuestionId());
        dto.setSource(record.getSource());
        dto.setSnapshotType(snapshot.getSnapshotType());
        dto.setContent(QuestionSnapshotCodec.text(view, "content"));
        dto.setShortTitle(snapshot.getShortTitle() != null
                ? snapshot.getShortTitle() : QuestionSnapshotCodec.text(view, "shortTitle"));
        dto.setType(snapshot.getQuestionType() != null
                ? snapshot.getQuestionType() : QuestionSnapshotCodec.integer(view, "type"));
        dto.setOptions(QuestionSnapshotCodec.stringList(view, "options"));
        dto.setRenderConfig(QuestionSnapshotCodec.node(view, "renderConfig"));
        dto.setCodeFramework(QuestionSnapshotCodec.text(view, "codeFramework"));
        dto.setRequirements(QuestionSnapshotCodec.stringList(view, "requirements"));
        dto.setHints(QuestionSnapshotCodec.stringList(view, "hints"));
        dto.setKnowledgePointIds(QuestionSnapshotCodec.longList(view, "knowledgePoints"));
        dto.setScore(snapshot.getScore() != null
                ? snapshot.getScore() : QuestionSnapshotCodec.integer(view, "score"));
        dto.setChapterId(snapshot.getChapterId() != null
                ? snapshot.getChapterId() : QuestionSnapshotCodec.longValue(view, "chapterId"));
        dto.setLastAnswer(record.getLastAnswer());

        dto.setAnswer(QuestionAnswerCodec.toText(QuestionSnapshotCodec.node(judge, "answer")));
        dto.setAnalysis(QuestionSnapshotCodec.text(judge, "analysis"));
        dto.setSolutionOutline(QuestionSnapshotCodec.text(judge, "solutionOutline"));
        dto.setJudgeMode(QuestionSnapshotCodec.integer(judge, "judgeMode"));
        dto.setTestCases(QuestionSnapshotCodec.objectList(judge, "testCases",
                WrongQuestionRetryDTO.TestCaseItem.class));
        return dto;
    }

    @Override
    public void markMastered(Long userId, Long wrongQuestionId) {
        ExamWrongQuestion record = requireOwned(userId, wrongQuestionId);
        record.setMastered(MASTERED);
        wrongQuestionMapper.updateById(record);
    }

    @Override
    public int markMasteredByQuestion(Long userId, Long questionId) {
        List<ExamQuestionSnapshot> snapshots = snapshotService.listByQuestionId(questionId);
        List<Long> snapshotIds = snapshots.stream()
                .map(ExamQuestionSnapshot::getId)
                .filter(Objects::nonNull)
                .toList();
        if (snapshotIds.isEmpty()) {
            throw new BusinessException(ErrorMessageConstant.WRONG_QUESTION_NOT_FOUND);
        }
        int updated = wrongQuestionMapper.update(null, Wrappers.<ExamWrongQuestion>lambdaUpdate()
                .eq(ExamWrongQuestion::getUserId, userId)
                .in(ExamWrongQuestion::getSnapshotId, snapshotIds)
                .set(ExamWrongQuestion::getMastered, MASTERED));
        if (updated == 0) {
            throw new BusinessException(ErrorMessageConstant.WRONG_QUESTION_NOT_FOUND);
        }
        return updated;
    }

    // ==================== 内部 ====================

    /**
     * 定位本次归档应挂到哪个快照：
     * 1) 调用方指定了快照（错题重做）→ 直接复用，保证仍挂在原来那一版；
     * 2) 作业批改带了 teach 快照ID → 按「作业发布快照」去重；
     * 3) 其余按题库当前内容算指纹，题改了就是新版本。
     */
    private ExamQuestionSnapshot resolveSnapshot(WrongQuestionArchiveRequest request) {
        if (request.getSnapshotId() != null && request.getSnapshotId() > 0) {
            ExamQuestionSnapshot pinned = snapshotService.getById(request.getSnapshotId());
            if (pinned != null) {
                return pinned;
            }
            log.warn("指定快照不存在，回退按题目重新生成: snapshotId={}", request.getSnapshotId());
        }
        ExamQuestion question = questionMapper.selectById(request.getQuestionId());
        if (question == null) {
            return null;
        }
        return snapshotService.resolve(question, request.getHomeworkSnapshotId());
    }

    /**
     * 题库最新版本（用于判断「题已被改过」）。查不到说明题已被删除，按不过期处理。
     */
    private Map<Long, ExamQuestion> loadLatestQuestions(Map<Long, ExamQuestionSnapshot> snapshotMap) {
        List<Long> questionIds = snapshotMap.values().stream()
                .map(ExamQuestionSnapshot::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        List<ExamQuestion> questions = questionMapper.selectBatchIds(questionIds);
        return (questions == null ? List.<ExamQuestion>of() : questions).stream()
                .collect(Collectors.toMap(ExamQuestion::getId, Function.identity()));
    }

    /**
     * 只有「题库版本」的快照才谈得上过期；作业快照本来就该固定在发布那一版。
     */
    private boolean isOutdated(ExamQuestionSnapshot snapshot, ExamQuestion latest) {
        if (snapshot == null || latest == null) {
            return false;
        }
        if (!Objects.equals(snapshot.getSnapshotType(), ExamQuestionSnapshot.TYPE_QUESTION_BANK)) {
            return false;
        }
        return !Objects.equals(snapshot.getFingerprint(), QuestionSnapshotCodec.fingerprint(latest));
    }

    private ExamWrongQuestion requireOwned(Long userId, Long wrongQuestionId) {
        ExamWrongQuestion record = wrongQuestionId == null ? null : wrongQuestionMapper.selectById(wrongQuestionId);
        if (record == null || !Objects.equals(record.getUserId(), userId)) {
            throw new BusinessException(ErrorMessageConstant.WRONG_QUESTION_NOT_FOUND);
        }
        return record;
    }

    private ExamQuestionSnapshot requireSnapshot(ExamWrongQuestion record) {
        ExamQuestionSnapshot snapshot = snapshotService.getById(record.getSnapshotId());
        if (snapshot == null) {
            log.warn("错题快照缺失: wrongQuestionId={}, snapshotId={}", record.getId(), record.getSnapshotId());
            throw new BusinessException(ErrorMessageConstant.WRONG_QUESTION_NOT_FOUND);
        }
        return snapshot;
    }

    private String truncate(String answer) {
        if (answer == null || answer.length() <= ANSWER_MAX_LENGTH) {
            return answer;
        }
        return answer.substring(0, ANSWER_MAX_LENGTH);
    }
}
