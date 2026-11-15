package com.amlve.study.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 学习行为记录（表：study_behavior）
 *
 * <p>为学情分析（agent-analysis）积累数据，由各链路异步埋点写入。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "study_behavior", autoResultMap = true)
public class StudyBehavior extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 学生ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 行为类型（BehaviorType.code）
     */
    @TableField("behavior_type")
    private String behaviorType;

    /**
     * 关联章节ID
     */
    @TableField("chapter_id")
    private Long chapterId;

    /**
     * 关联题目ID
     */
    @TableField("question_id")
    private Long questionId;

    /**
     * 行为耗时（秒）
     */
    @TableField("duration")
    private Integer duration;

    /**
     * 行为详情（JSON）
     */
    @TableField(value = "detail", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> detail;
}
