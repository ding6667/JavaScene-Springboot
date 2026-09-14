package com.amlve.study.dto.response;

import lombok.Data;
import java.util.List;

/**
 * 代码运行结果 VO（返回给前端）
 */
@Data
public class CodeRunResultResponse {

    /**
     * 记录ID
     */
    private Long runId;

    /**
     * 运行状态：1=成功，2=编译错误，3=运行时错误，4=未通过安全检查，5=系统内部错误
     */
    private Integer status;

    /**
     * 程序标准输出（成功或运行时错误时返回）
     */
    private String output;

    /**
     * 错误信息（编译错误 / 运行时错误 / 安全拦截 / 系统错误时返回）
     */
    private String errorMsg;

    /**
     * 错误行号（编译/运行时报错时返回）
     */
    private Integer errorLine;

    /**
     * 运行耗时（毫秒）
     */
    private Integer runDuration;

    // ========== AI 调试返回（仅在出错时填充） ==========

    /**
     * 错误原因（人话解释）
     */
    private String errorReason;

    /**
     * 优化建议
     */
    private String optimizeSuggest;

    /**
     * 标准解题思路
     */
    private String standardIdea;

    /**
     * 关联知识点列表
     */
    private List<String> knowledgePoints;
}