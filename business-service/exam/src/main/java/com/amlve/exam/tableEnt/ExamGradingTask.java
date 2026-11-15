package com.amlve.exam.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.amlve.exam.enumeration.ExamGradingTaskStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 作业批改任务表实体（按 学生+作业 汇总）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_grading_task")
public class ExamGradingTask extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("homework_id")
    private Long homeworkId;

    @TableField("user_id")
    private Long userId;

    @TableField("total_score")
    private Integer totalScore;

    @TableField("student_score")
    private Integer studentScore;

    @TableField(value = "score_detail", typeHandler = JacksonTypeHandler.class)
    private List<ScoreDetailItem> scoreDetail;

    private ExamGradingTaskStatus status;

    @TableField("error_message")
    private String errorMessage;

    /**
     * 单题得分与错误原因
     */
    @Data
    public static class ScoreDetailItem {
        private Long questionId;
        private String questionContent;
        private Integer fullScore;
        private Integer score;
        private String errorReason;
    }
}