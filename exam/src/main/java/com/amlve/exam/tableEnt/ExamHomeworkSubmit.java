package com.amlve.exam.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 作业提交表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_homework_submit")
public class ExamHomeworkSubmit extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("homework_id")
    private Long homeworkId;

    @TableField("user_id")
    private Long userId;

    @TableField("question_id")
    private Long questionId;

    /**
     * 作业题目快照ID（teach_homework_question.id）。
     * 批改时据此把错题钉在「发布那一版题」上；0 表示未上报，按题库版本归档。
     */
    @TableField("snapshot_id")
    private Long snapshotId;

    @TableField("answer_content")
    private String answerContent;

    @TableField("submit_time")
    private LocalDateTime submitTime;
}