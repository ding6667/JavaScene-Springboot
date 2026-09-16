package com.amlve.knowledge.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 保存知识点权重配置请求 DTO
 * <p>
 * 前端提交的"收货单"，字段校验（jakarta.validation）在此层完成，
 * 非法数据在进入 Service 前即被拦截（Controller 方法参数需加 @Valid）。
 */
@Data
public class WeightConfigSaveRequest {

    /**
     * 关联章节ID（必填）
     */
    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    /**
     * 关联知识点ID（必填）
     */
    @NotNull(message = "知识点ID不能为空")
    private Long knowledgePointId;

    /**
     * 权重值（必填，1.00-5.00）
     */
    @NotNull(message = "权重值不能为空")
    @DecimalMin(value = "1.00", message = "权重值不能低于1.00")
    @DecimalMax(value = "5.00", message = "权重值不能超过5.00")
    private BigDecimal weight;

    /**
     * 基础出题数量（必填，至少1题）
     */
    @NotNull(message = "基础出题数量不能为空")
    @Min(value = 1, message = "基础出题数量至少为1")
    private Integer baseQuestionCount;

    /**
     * 最大出题数量（必填，不高于50）
     */
    @NotNull(message = "最大出题数量不能为空")
    @Min(value = 1, message = "最大出题数量至少为1")
    @Max(value = 50, message = "最大出题数量不能超过50")
    private Integer maxQuestionCount;

    /**
     * 难度分布百分比（选填，格式 "简单,中等,困难" 如 "30,50,20"，三项之和应为100）
     */
    @Pattern(regexp = "^$|^([0-9]{1,2}|100)(,([0-9]{1,2}|100)){2}$",
            message = "难度分布格式应为三项百分比，如 30,50,20")
    private String difficultyDistribution;

    /**
     * 是否启用（选填，默认 true）
     */
    private Boolean isActive;
}
