package com.amlve.ai.tableEnt;

import com.amlve.ai.enumeration.GradingTaskStatus;
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
    //批阅评分标准id
    private String rubricId;
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
}