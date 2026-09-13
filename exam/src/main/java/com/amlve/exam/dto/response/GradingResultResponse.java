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

    private Long id;

    private Long homeworkId;

    private Long userId;

    private Integer totalScore;

    private Integer studentScore;

    private List<ExamGradingTask.ScoreDetailItem> scoreDetail;

    private ExamGradingTaskStatus status;

    private String errorMessage;
}