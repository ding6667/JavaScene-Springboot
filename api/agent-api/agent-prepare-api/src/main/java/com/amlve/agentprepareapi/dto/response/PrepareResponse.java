package com.amlve.agentprepareapi.dto.response;

import lombok.Data;

@Data
public class PrepareResponse {

    /**
     * 教案内容（Markdown）
     */
    private String lessonPlan;

    /**
     * PPT
     */
    private String pptx;

    /**
     * 重难点清单（Markdown）
     */
    private String keyPoints;

    /**
     * 课后习题（Markdown）
     */
    private String homework;

    /**
     * 上机实训方案（Markdown）
     */
    private String lab;

    /**
     * 备课记录ID
     */
    private Long recordId;
}