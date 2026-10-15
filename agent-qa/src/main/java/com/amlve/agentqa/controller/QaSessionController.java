package com.amlve.agentqa.controller;

import com.amlve.agentqaapi.dto.request.QARequest;
import com.amlve.agentqaapi.dto.request.QaHistoryQueryRequest;
import com.amlve.agentqaapi.dto.response.QAResponse;
import com.amlve.agentqaapi.dto.response.QaHistoryVO;
import com.amlve.agentqaapi.service.QaAgent;
import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.common.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 答疑同步接口与问答历史（任务 5）
 * <p>
 * 权限口径按任务书第九节：提问与历史查询限 STUDENT，且 userId 一律取自登录令牌，
 * 不接受前端传 userId 当查询条件——否则换个参数就能读到别人的问答。
 * <p>
 * 与流式接口 {@link QAAnswerController} 共用同一套上下文与落库逻辑，
 * 前端最终用哪条由 11.3 定；两条并存期间数据口径一致，不会各写各的。
 * <p>
 * 归属说明：任务书把 Controller 画在 study 模块（蔡剑飞），但 6.18.7 的口径是
 * "qa_record 读写归 agent-qa，其它模块经 QaAgent 接口调用"。故本模块先落地这三个入口，
 * study 侧若需要 {@code /api/study/qa/**} 的同义路由，注入 QaAgent 三行转发即可，
 * 不要在 study 里再建一份 qa_record 实体。
 */
@Slf4j
@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class QaSessionController {

    private final QaAgent qaAgent;

    /**
     * 学生提问（同步返回完整答案）
     * <p>
     * POST /api/qa/ask  body: {"question":"什么是封装","chapterId":5,"sessionId":可选}
     * <p>
     * 返回体带 sessionId 与 roundNo：首轮不传 sessionId，把响应里的存下来，
     * 后续同一会话带上它才能拿到多轮上下文（Redis 里 30 分钟内有效）。
     */
    @PostMapping("/ask")
    public Result<QAResponse> ask(@Valid @RequestBody QARequest request) {
        Long userId = SecurityUtils.getUserId();
        log.info("学生提问: userId={}, sessionId={}, chapterId={}",
                userId, request.getSessionId(), request.getChapterId());
        return qaAgent.answer(userId, request);
    }

    /**
     * 分页查询本人历史问答
     * <p>
     * GET /api/qa/history?chapterId=5&pageNum=1&pageSize=10
     * GET /api/qa/history?sessionId=xxx —— 带会话ID时按 round_no 升序还原那一次完整对话
     */
    @GetMapping("/history")
    public Result<PageResult<QaHistoryVO>> history(QaHistoryQueryRequest request) {
        Long userId = SecurityUtils.getUserId();
        return qaAgent.history(userId, request);
    }
}
