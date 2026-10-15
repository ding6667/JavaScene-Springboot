package com.amlve.teachapi.dto.response;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 作业提交校验结果（供 exam 调）
 */
@Data
public class HomeworkSubmitCheckResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long homeworkId;
    private Boolean submittable;
    private Integer status;
    private String statusName;
    private LocalDateTime deadline;
    private Boolean expired;
    private Integer allowLateSubmit;
    private String reason;
}