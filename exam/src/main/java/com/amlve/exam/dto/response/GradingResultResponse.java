package com.amlve.exam.dto.response;

import com.amlve.exam.enumeration.ExamGradingTaskStatus;
import com.amlve.exam.tableEnt.ExamGradingTask;
import lombok.Data;

import java.util.List;

/**
 * 批改结果查询响应
 */
@Data
public class GradingResultResponse {

    /** 批改任务 ID */
    private Long id;

    /** 作业 ID */
    private Long homeworkId;

    /** 学生 ID */
    private Long userId;

    /** 作业总分 */
    private Integer totalScore;

    /** 学生得分 */
    private Integer studentScore;

    /** 每题得分与错误原因明细 */
    private List<ExamGradingTask.ScoreDetailItem> scoreDetail;

    /** 批改状态（待批改/已完成） */
    private ExamGradingTaskStatus status;

    /** 错误信息（批改失败时） */
    private String errorMessage;
}