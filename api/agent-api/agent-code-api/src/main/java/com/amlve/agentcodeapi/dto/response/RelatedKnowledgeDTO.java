package com.amlve.agentcodeapi.dto.response;

import lombok.Data;

/**
 * 关联教材（章节 + 页码）
 */
@Data
public class RelatedKnowledgeDTO {

    /**
     * 知识点名称
     */
    private String point;

    /**
     * 章节名称
     */
    private String chapter;

    /**
     * 页码
     */
    private String page;
}
