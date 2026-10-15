package com.amlve.studyapi.dto.response;

import lombok.Data;

@Data
public class ProgrammingJudgeResponse {
    private Integer score;

    private Boolean passed;

    private Integer passedCount;

    private Integer totalCount;

    private String output;

    private String errorMsg;

    private String errorReason;

    private String analysis;

    private String knowledgePoints;

    private Long runRecordId;
}
