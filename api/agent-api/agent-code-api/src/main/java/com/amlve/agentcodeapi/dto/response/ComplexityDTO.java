package com.amlve.agentcodeapi.dto.response;

import lombok.Data;

/**
 * 复杂度分析
 */
@Data
public class ComplexityDTO {

    /**
     * 时间复杂度，如 O(n)
     */
    private String timeComplexity;

    /**
     * 空间复杂度，如 O(1)
     */
    private String spaceComplexity;

    /**
     * 复杂度分析说明
     */
    private String analysis;

    /**
     * 是否可优化
     */
    private Boolean optimizable;

    /**
     * 优化方向
     */
    private String optimizeDirection;
}
