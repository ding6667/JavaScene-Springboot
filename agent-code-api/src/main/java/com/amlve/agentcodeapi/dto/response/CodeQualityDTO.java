package com.amlve.agentcodeapi.dto.response;

import lombok.Data;

/**
 * 代码质量评分（各维度 1-5 分）
 *
 * <p>注意 {@code comment} 字段的双关口径（任务书原稿两字段重名）：
 * <ul>
 *   <li>Dify 出参 {@code comment} 为<b>数值</b>时，表示「注释质量」维度分值，落在 {@link #comment}</li>
 *   <li>Dify 出参 {@code comment} 为<b>字符串</b>或走 {@code review} 键时，表示「各维度点评」，落在 {@link #review}</li>
 * </ul>
 */
@Data
public class CodeQualityDTO {

    /**
     * 可读性（1-5分）
     */
    private Integer readability;

    /**
     * 命名规范（1-5分）
     */
    private Integer naming;

    /**
     * 代码结构（1-5分）
     */
    private Integer structure;

    /**
     * 注释质量（1-5分）
     */
    private Integer comment;

    /**
     * 复杂度控制（1-5分）
     */
    private Integer complexityControl;

    /**
     * 可复用性（1-5分）
     */
    private Integer reusability;

    /**
     * 综合评分（1-5分）
     */
    private Integer overall;

    /**
     * 各维度点评
     */
    private String review;
}
