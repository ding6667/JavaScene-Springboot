package com.amlve.studyapi.dto.response;

import lombok.Data;

@Data
public class PracticeStatusResponse {

    private Long questionId;

    private Boolean done;

    private Boolean correct;

    private Integer attemptCount;

    private String lastAnswer;

    private Boolean favorited;
}
