package com.amlve.agentqaapi.service;

import com.amlve.agentqaapi.dto.request.QARequest;
import com.amlve.agentqaapi.dto.request.QaHistoryQueryRequest;
import com.amlve.agentqaapi.dto.response.QAResponse;
import com.amlve.agentqaapi.dto.response.QaHistoryVO;
import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;

/**
 * 答疑智能体对外接口（供 study 等模块跨模块调用）
 * <p>
 * 口径说明（对应任务书 6.18.7）：{@code qa_record} 的读写一律归 agent-qa 模块，
 * study 只通过本接口调用。若在 study 里再建一份同表实体，同一张表就会有两个模块
 * 各自按自己的字段命名改来改去，最后谁都不敢动。
 * <p>
 * userId 由调用方从登录令牌取出后传入：本接口不做鉴权判定，避免智能体层依赖 Web 上下文。
 */
public interface QaAgent {

    /**
     * 单轮答疑（同步返回）。
     * <p>
     * 内部完成：读 Redis 多轮上下文 → 组装带历史的工作流入参 → 调 Dify →
     * 用知识库校验口径回填教材引用与是否超纲 → 追加 Redis 上下文 → 落库 qa_record。
     * 上下文与落库失败都不打断主流程，学生拿到答案优先。
     *
     * @param userId  提问学生ID
     * @param request 提问请求（问题、章节ID、会话ID）
     * @return 答案与本轮元数据（sessionId / roundNo 供前端续接会话）
     */
    Result<QAResponse> answer(Long userId, QARequest request);

    /**
     * 分页查询本人历史问答
     *
     * @param userId  当前登录学生ID（服务端取值，不接受前端传参）
     * @param request 查询条件（章节 / 会话可选筛选）
     * @return 分页结果，按创建时间倒序；查单个会话时按 round_no 升序还原对话
     */
    Result<PageResult<QaHistoryVO>> history(Long userId, QaHistoryQueryRequest request);
}
