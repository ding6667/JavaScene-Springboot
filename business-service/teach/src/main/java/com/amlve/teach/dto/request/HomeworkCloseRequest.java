package com.amlve.teach.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 教师关闭作业的请求参数
 */
@Data
public class HomeworkCloseRequest {

    @NotNull(message = "作业ID不能为空")
    private Long homeworkId;

    /** 关闭原因，可选（数据库列长度 200） */
    @Size(max = 200, message = "关闭原因不能超过200字")
    private String closeReason;
}
