package com.amlve.agentqaapi.dto.request;

import com.amlve.common.constant.ErrorMessageConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 学生答疑请求
 */
@Data
public class QARequest {

    /**
     * 问题内容
     */
    @NotBlank(message = ErrorMessageConstant.QA_QUESTION_EMPTY)
    @Size(max = 2000, message = ErrorMessageConstant.QA_QUESTION_TOO_LONG)
    private String question;

    /**
     * 章节ID（可选）。答疑工作流按该章节的教材内容作答，不传走"未指定章节"逻辑
     */
    private Long chapterId;

    /**
     * 会话ID（可选）。首次提问不传，由后端生成 UUID 并在响应里回传，
     * 后续同一轮对话带上它，round_no 才能连续
     */
    @Size(max = 64, message = ErrorMessageConstant.QA_SESSION_STALE)
    private String sessionId;
}
