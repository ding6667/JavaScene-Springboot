package com.amlve.agentqa.controller;

import com.amlve.agentqa.service.impl.QaAgentImpl;
import com.amlve.agentqaapi.service.QAAgentClient;
import com.amlve.common.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.List;

/**
 * 答疑智能体 SSE 流式接口（对应测试报告 S-08）
 * <p>
 * GET 便于前端 EventSource 直连（EventSource 只支持 GET）。
 * 事件协议见 {@link AnswerStreamSplitter}：think-start / think / think-end 三个事件
 * 承载模型思考过程，message 承载正式答案，error 承载失败原因；
 * data 一律为纯文本片段，不做 JSON 包装，前端直接 append 即可。
 * <p>
 * 本流在 message 之前额外发一个 {@value #EVENT_META} 事件（JSON：sessionId + roundNo），
 * 前端必须存下来并在下一次提问时带上，多轮对话才有会话可续。
 * <p>
 * 与同步答疑 {@code POST /api/qa/ask} 共用同一套上下文与落库逻辑（任务书 6.18.6）：
 * 流结束时才追加上下文、写 qa_record，<b>客户端中途断开就两边都不写</b>——
 * 半截答案落库会污染历史记录与学情统计，宁缺不假。
 * <p>
 * 前端注意两点：一是 EventSource 断线会自动重连并重复提问，答疑场景建议关闭自动重连
 * （否则同一问题会被计入两轮）；二是正文里的 Markdown（含代码块）由前端渲染器负责，
 * 流式期间需对未闭合结构补虚拟闭合。
 * <p>
 * 权限限 STUDENT，与 {@code /api/qa/ask}、{@code /api/qa/history} 同口径（任务书第九节：
 * 提问与问答历史都只属于学生，教师侧要试听请用同步接口）。
 */
@Slf4j
@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class QAAnswerController {

    /**
     * SSE 流超时（毫秒）：答疑工作流含检索与大模型生成，且思考链全程下发，耗时较长
     */
    private static final long SSE_TIMEOUT_MS = 180_000L;

    /**
     * 错误事件名
     */
    private static final String EVENT_ERROR = "error";

    /**
     * 会话元数据事件名（在正文之前发一次）
     */
    private static final String EVENT_META = "meta";

    /**
     * 正文事件名（与 {@link AnswerStreamSplitter} 约定一致）
     */
    private static final String EVENT_MESSAGE = "message";

    private final QAAgentClient qaAgentClient;

    /**
     * 直接注入实现类而不是 QaAgent 接口：分段/收尾用的 beginTurn、finishStreamTurn
     * 是流式通道专用的过程态方法，不适合放进跨模块接口里污染 api 契约
     */
    private final QaAgentImpl qaAgent;

    /**
     * 答疑流式问答
     * <p>
     * GET /api/qa/answer/stream?question=什么是多态&chapterId=5&sessionId=xxx
     *
     * @param question  学生提问内容
     * @param chapterId 章节ID（可选，限定答疑范围）
     * @param sessionId 会话ID（可选，首轮不传，由 meta 事件回传后带上以续接多轮）
     */
    @GetMapping("/answer/stream")
    public SseEmitter answerStream(@RequestParam String question,
                                   @RequestParam(required = false) String chapterId,
                                   @RequestParam(required = false) String sessionId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        // 必须在请求线程取用户ID：订阅回调跑在 reactor 线程上，SecurityContext 不会跟过去
        Long userId = SecurityUtils.getUserId();
        QaAgentImpl.QaTurn turn = qaAgent.beginTurn(userId, toChapterId(chapterId), sessionId, question);
        // 每个连接一个状态机实例（其内部缓冲非线程安全）
        AnswerStreamSplitter splitter = new AnswerStreamSplitter();
        // 累积正文帧，流结束时整段用于上下文追加与落库；只有 message 帧算答案，think 帧不算
        StringBuilder answerText = new StringBuilder();
        if (!sendMeta(emitter, turn)) {
            return emitter;
        }
        qaAgentClient.answerStream(chapterId, turn.prompt())
                // 回调里的落库与校验是阻塞 JDBC，挪出 Netty 事件循环线程
                .publishOn(Schedulers.boundedElastic())
                .subscribe(
                        chunk -> {
                            try {
                                List<AnswerStreamSplitter.Frame> frames = splitter.accept(chunk);
                                for (AnswerStreamSplitter.Frame frame : frames) {
                                    if (EVENT_MESSAGE.equals(frame.event())) {
                                        answerText.append(frame.data());
                                    }
                                }
                                sendAll(emitter, frames);
                            } catch (IOException e) {
                                // 前端已断开：抛出让订阅取消，避免向失效连接继续推送
                                throw new IllegalStateException("SSE 客户端已断开", e);
                            }
                        },
                        error -> {
                            log.error("答疑流式推送失败: userId={}, question={}", userId, question, error);
                            try {
                                // 先收尾再报错：否则思考块未闭合时前端"思考中"面板会一直转圈
                                sendAll(emitter, splitter.finish());
                                emitter.send(SseEmitter.event().name(EVENT_ERROR)
                                        .data("答疑失败: " + error.getMessage(), MediaType.TEXT_PLAIN));
                            } catch (IOException | IllegalStateException ignored) {
                                // 客户端已断开，错误事件无法送达
                            }
                            // 失败/断开都不落库：本轮没有完整答案，写了就是半截记录
                            emitter.complete();
                        },
                        () -> {
                            try {
                                sendAll(emitter, splitter.finish());
                            } catch (IOException ignored) {
                                // 客户端已断开，收尾帧无法送达
                            }
                            // 答案已完整推送，此时才计入上下文与问答记录
                            try {
                                qaAgent.finishStreamTurn(turn, answerText.toString());
                            } catch (Exception e) {
                                log.error("流式答疑收尾落库失败: sessionId={}, roundNo={}",
                                        turn.sessionId(), turn.roundNo(), e);
                            }
                            emitter.complete();
                        });
        return emitter;
    }

    /**
     * 下发会话元数据。客户端在建档这一瞬就断开时直接结束流，不再浪费一次工作流调用
     *
     * @return true 表示连接可用、可以继续推流
     */
    private boolean sendMeta(SseEmitter emitter, QaAgentImpl.QaTurn turn) {
        try {
            emitter.send(SseEmitter.event().name(EVENT_META)
                    .data(new QaStreamMeta(turn.sessionId(), turn.roundNo()), MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException | IllegalStateException e) {
            log.warn("SSE 会话元数据下发失败，客户端可能已断开: sessionId={}", turn.sessionId());
            emitter.complete();
            return false;
        }
    }

    /**
     * 章节ID参数转 Long：非数字（历史调用方传章节编号字符串）返回 null，
     * 上下文与教材校验按"不限章节"处理，原始字符串仍照旧传给 Dify 工作流
     */
    private Long toChapterId(String chapterId) {
        if (chapterId == null || chapterId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(chapterId.trim());
        } catch (NumberFormatException e) {
            log.warn("章节ID非数字，本轮问答不做上下文隔离与教材校验: chapterId={}", chapterId);
            return null;
        }
    }

    /**
     * 按事件名逐帧推送，data 为纯文本
     */
    private void sendAll(SseEmitter emitter, List<AnswerStreamSplitter.Frame> frames) throws IOException {
        for (AnswerStreamSplitter.Frame frame : frames) {
            emitter.send(SseEmitter.event().name(frame.event())
                    .data(frame.data(), MediaType.TEXT_PLAIN));
        }
    }

    /**
     * meta 事件载荷
     *
     * @param sessionId 会话ID，前端存下并在下次提问时带上
     * @param roundNo   本轮在该会话中的序号
     */
    private record QaStreamMeta(String sessionId, Integer roundNo) {
    }
}
