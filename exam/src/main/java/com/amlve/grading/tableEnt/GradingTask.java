package com.amlve.grading.tableEnt;

import com.amlve.grading.enumeration.GradingTaskStatus;
import com.amlve.grading.enumeration.TaskCategory;
import com.amlve.grading.enumeration.FileType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 批阅任务表实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("grading_task")
public class GradingTask {
    //批阅任务ID（由程序生成）
    @TableId(type = IdType.INPUT)
    private String id;
    //关联的上传文件ID
    private String fileId;
    //关联的用户ID
    private Long userId;
    //批阅评分标准id
    private Long rubricId;
    //学生总人数
    private Integer studentCount;
    //已处理学生人数
    private Integer processedStudentCount;
    //任务状态：0-待处理 1-处理中 2-已完成 3-已过期 4-失败(可重试)
    private GradingTaskStatus status;
    //重试次数
    private Integer retryCount;
    //错误信息（最后一次失败时的异常信息）
    private String errorMessage;
    //任务创建时间
    private LocalDateTime createTime;
    //完成时间
    private LocalDateTime finishTime;
    //是否删除：0-未删除 1-已删除
    private Integer deleted;
    //任务类别: HOMEWORK(平时), MIDTERM(期中), FINAL(期末), PROJECT(项目)
    private TaskCategory taskCategory;
    //该类任务下的序号，如第1次平时作业就填1
    private Integer taskSequence;
    //所属班级ID
    private String classId;
    //学期，如 2026-01
    private String semester;
    //学生ID（word文件对应单个学生）
    private String studentId;
    //文件类型: WORD/ZIP
    private FileType fileType;
}