package com.amlve.grading.vo;

import com.amlve.grading.enumeration.GradingTaskStatus;
import com.amlve.grading.enumeration.TaskCategory;
import com.amlve.grading.enumeration.FileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingTaskResponse {
    private String id;
    private String fileId;
    private Long userId;
    private Long rubricId;
    private Integer studentCount;
    private Integer processedStudentCount;
    private GradingTaskStatus status;
    private Integer retryCount;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
    private TaskCategory taskCategory;
    private Integer taskSequence;
    private String classId;
    private String semester;
    private String studentId;
    private FileType fileType;
    private String fileName;
}