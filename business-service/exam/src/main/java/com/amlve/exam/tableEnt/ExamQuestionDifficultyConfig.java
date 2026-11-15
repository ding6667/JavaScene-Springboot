package com.amlve.exam.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 出题难度配置表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "exam_question_difficulty_config", autoResultMap = true)
public class ExamQuestionDifficultyConfig extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 难度编码，如 basic/medium/hard/custom_001
     */
    private String code;

    /**
     * 难度名称，如 简单、中等、困难
     */
    private String name;

    /**
     * 难度说明
     */
    private String description;

    /**
     * 题目示例
     */
    private String example;

    /**
     * 难度排序，1最简单
     */
    @TableField("level_order")
    private Integer levelOrder;

    /**
     * 认知层次，如 ["记忆","理解"]
     */
    @TableField(value = "cognitive_levels", typeHandler = JacksonTypeHandler.class)
    private List<String> cognitiveLevels;

    /**
     * 最少知识点数量
     */
    @TableField("knowledge_point_min")
    private Integer knowledgePointMin;

    /**
     * 最多知识点数量
     */
    @TableField("knowledge_point_max")
    private Integer knowledgePointMax;

    /**
     * 是否允许跨知识点，0否1是
     */
    @TableField("allow_cross_points")
    private Integer allowCrossPoints;

    /**
     * 最少推理步骤
     */
    @TableField("reasoning_steps_min")
    private Integer reasoningStepsMin;

    /**
     * 最多推理步骤
     */
    @TableField("reasoning_steps_max")
    private Integer reasoningStepsMax;

    /**
     * 最少代码行数
     */
    @TableField("code_lines_min")
    private Integer codeLinesMin;

    /**
     * 最多代码行数
     */
    @TableField("code_lines_max")
    private Integer codeLinesMax;

    /**
     * 题干最大字数
     */
    @TableField("question_length_max")
    private Integer questionLengthMax;

    /**
     * 选项数量
     */
    @TableField("option_count")
    private Integer optionCount;

    /**
     * 干扰项强度：low/medium/high
     */
    @TableField("distractor_strength")
    private String distractorStrength;

    /**
     * 难度分下限
     */
    @TableField("score_min")
    private BigDecimal scoreMin;

    /**
     * 难度分上限
     */
    @TableField("score_max")
    private BigDecimal scoreMax;

    /**
     * 是否系统预设
     */
    @TableField("is_system")
    private Integer isSystem;

    /**
     * 教师ID，NULL表示系统级
     */
    @TableField("teacher_id")
    private Long teacherId;

    /**
     * 绑定章节，NULL表示通用
     */
    @TableField("chapter_id")
    private Long chapterId;

    /**
     * 1启用 0禁用
     */
    private Integer status;

    /**
     * 排序序号
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
