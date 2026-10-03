package com.amlve.agentqa.controller;

import com.amlve.agentqaapi.service.QAAgentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
 * 前端注意两点：一是 EventSource 断线会自动重连并重复提问，答疑场景建议关闭自动重连；
 * 二是正文里的 Markdown（含代码块）由前端渲染器负责，流式期间需对未闭合结构补虚拟闭合。
 */
@Slf4j
@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class QAAnswerController {

    /**
     * SSE 流超时（毫秒）：答疑工作流含检索与大模型生成，且思考链全程下发，耗时较长
     */
    private static final long SSE_TIMEOUT_MS = 180_000L;

    /**
     * 错误事件名
     */
    private static final String EVENT_ERROR = "error";

    private final QAAgentClient qaAgentClient;

    /**
     * 答疑流式问答
     * <p>
     * GET /api/qa/answer/stream?question=什么是多态&chapterId=5
     *
     * @param question  学生提问内容
     * @param chapterId 章节ID（可选，限定答疑范围）
     */
    @GetMapping("/answer/stream")
    public SseEmitter answerStream(@RequestParam String question,
                                   @RequestParam(required = false) String chapterId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        // 每个连接一个状态机实例（其内部缓冲非线程安全）
        AnswerStreamSplitter splitter = new AnswerStreamSplitter();
        qaAgentClient.answerStream(chapterId, question).subscribe(
                chunk -> {
                    try {
                        sendAll(emitter, splitter.accept(chunk));
                    } catch (IOException e) {
                        // 前端已断开：抛出让订阅取消，避免向失效连接继续推送
                        throw new IllegalStateException("SSE 客户端已断开", e);
                    }
                },
                error -> {
                    log.error("答疑流式推送失败: question={}", question, error);
                    try {
                        // 先收尾再报错：否则思考块未闭合时前端"思考中"面板会一直转圈
                        sendAll(emitter, splitter.finish());
                        emitter.send(SseEmitter.event().name(EVENT_ERROR)
                                .data("答疑失败: " + error.getMessage(), MediaType.TEXT_PLAIN));
                    } catch (IOException | IllegalStateException ignored) {
                        // 客户端已断开，错误事件无法送达
                    }
                    emitter.complete();
                },
                () -> {
                    try {
                        sendAll(emitter, splitter.finish());
                    } catch (IOException ignored) {
                        // 客户端已断开，收尾帧无法送达
                    }
                    emitter.complete();
                });
        return emitter;
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
}
