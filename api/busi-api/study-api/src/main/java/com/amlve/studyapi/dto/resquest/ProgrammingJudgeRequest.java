package com.amlve.studyapi.dto.resquest;


import lombok.Data;

@Data
public class ProgrammingJudgeRequest {

    /**
     * exam_homework_submit.id
     */
    private Long submitId;

    private Long userId;

    private Long questionId;

    private String codeContent;
}
