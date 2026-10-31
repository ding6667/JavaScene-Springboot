package com.amlve.exam.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 难度配置视图（前端出题/组卷时选择难度用）。
 * <p>
 * 只暴露选择所需字段：编码、名称、排序与难度分区间；认知层次供展示提示，
 * 推理步数/代码行数等约束留给出题智能体，不下发给前端。
 */
@Data
public class DifficultyConfigResponse {

    /** 配置ID，抽题与题库落库按此关联 */
    private Long id;

    /** 难度编码，如 basic/intermediate/advanced */
    private String code;

    /** 难度名称，如 简单/进阶/拔高 */
    private String name;

    /** 难度说明 */
    private String description;

    /** 示例题干 */
    private String example;

    /** 难度排序，1最简单 */
    private Integer levelOrder;

    /** 认知层次 */
    private List<String> cognitiveLevels;

    /** 难度分下限 */
    private BigDecimal scoreMin;

    /** 难度分上限 */
    private BigDecimal scoreMax;

    /** 是否系统预设：1是 0否（教师自建） */
    private Integer isSystem;

    /** 绑定章节ID，NULL 表示通用 */
    private Long chapterId;
}
