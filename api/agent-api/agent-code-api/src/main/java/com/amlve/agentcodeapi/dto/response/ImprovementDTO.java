package com.amlve.agentcodeapi.dto.response;

import lombok.Data;

/**
 * 代码改进建议
 */
@Data
public class ImprovementDTO {

    /**
     * 改进类型：READABILITY / PERFORMANCE / EDGE_CASE / REUSABILITY
     */
    private String type;

    /**
     * 改进描述
     */
    private String description;

    /**
     * 改进示例代码
     */
    private String example;

    /**
     * 优先级：HIGH / MEDIUM / LOW
     */
    private String priority;
}
