package com.amlve.agentcodeapi.dto.response;

import lombok.Data;

import java.util.List;

/**
 * Dify 代码调试响应
 */
@Data
public class CodeDebugResponse {

    /**
     * 错误行号列表
     */
    private List<String> errorLineList;

    /**
     * 错误原因（人话解释）
     */
    private String errorReason;

    /**
     * 优化建议（引导式）
     */
    private String optimizeSuggest;

    /**
     * 标准解题思路
     */
    private String standardIdea;

    /**
     * 关联的教材知识点
     */
    private List<String> knowledgePoints;
}