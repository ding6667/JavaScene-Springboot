package com.amlve.agentqaapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dify 答疑工作流的执行结果
 * <p>
 * 比裸答案多了 qa_record 需要持久化的三列（教材引用除外，引用由 knowledge 侧校验给出）：
 * dify_message_id 用于回溯 Dify 侧那次执行，tokensUsed 是成本与限流分析的唯一数据来源，
 * responseTime 配合答疑超时口径做慢查询分析。这三列若无人赋值，建表就是白建。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QaWorkflowAnswerVO {

    /**
     * 答案正文（已剥除 &lt;think&gt; 思考链）
     */
    private String answer;

    /**
     * Dify 侧执行标识（工作流的 workflow_run_id / 执行 id），取不到时为 null
     */
    private String difyMessageId;

    /**
     * 本轮消耗 token 数，取不到时为 null（不写 0，避免与"确实消耗 0 token"混淆）
     */
    private Integer tokensUsed;

    /**
     * Dify 侧统计的执行耗时（毫秒），取不到时为 null
     */
    private Integer responseTime;
}
