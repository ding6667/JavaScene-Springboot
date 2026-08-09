package com.amlve.ai.tableEnt;

import com.amlve.ai.entity.CommentJson;
import com.amlve.ai.enumeration.GradingTaskStatus;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学生批阅结果表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("student_result")
public class StudentResult {

    /**
     * 自增主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的任务ID
     */
    private String taskId;

    /**
     * 学生学号
     */
    private String studentId;

    /**
     * 学生姓名
     */
    private String studentName;

    /**
     * AI原始评分
     */
    private String rawScore;

    /**
     * AI批阅评语(Markdown格式)
     */
    private String aiComment;
    /**
     * AI批阅评语(JSON格式)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private CommentJson commentJson;
    /**
     * 教师修改后的分数
     */
    private String teacherScore;

    /**
     * 教师批阅评语
     */
    private String teacherComment;

    /**
     * 教师备注
     */
    private String teacherNote;

    /**
     * 状态: PENDING=待批阅, PROCESSING=处理中, APPROVED=已批阅, FAILED=失败
     */
    private GradingTaskStatus status;

    /**
     * 错误信息(处理失败时记录)
     */
    private String errorMessage;

    /**
     * 学生作业路径(Markdown格式,用于重试)
     */
    private String wordContentPath;

    /**
     * 创建时间（由数据库自动生成）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间（由数据库自动生成）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


    private Integer deleted;
}