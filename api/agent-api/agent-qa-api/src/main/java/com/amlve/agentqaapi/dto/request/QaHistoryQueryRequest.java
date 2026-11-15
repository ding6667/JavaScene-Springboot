package com.amlve.agentqaapi.dto.request;

import lombok.Data;

/**
 * 问答历史分页查询条件
 * <p>
 * 不含 userId：数据隔离由服务端从登录令牌取，前端传谁都查不到别人的记录。
 */
@Data
public class QaHistoryQueryRequest {

    /**
     * 章节ID（可选）
     */
    private Long chapterId;

    /**
     * 会话ID（可选，查某一次完整对话，按 round_no 升序还原）
     */
    private String sessionId;

    /**
     * 页码，从 1 开始
     */
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 10;
}
