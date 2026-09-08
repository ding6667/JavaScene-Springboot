package com.amlve.teach.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 教师生成教案的请求参数（前端提交用）
 */
@Data
public class PrepareRequest {

    @NotNull(message = "章节ID不能为空")
    private Long chapterId;
}
