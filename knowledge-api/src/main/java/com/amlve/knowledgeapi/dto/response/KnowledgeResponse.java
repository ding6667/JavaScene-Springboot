package com.amlve.knowledgeapi.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 知识点检索响应（供前端展示）
 */
@Data
public class KnowledgeResponse {

    /**
     * 检索关键词
     */
    private String query;

    /**
     * 检索结果列表
     */
    private List<KnowledgeItem> items;

    /**
     * 业务提示文案（如"未找到相关知识"）
     */
    private String message;

    /**
     * 单条检索结果
     */
    @Data
    public static class KnowledgeItem {

        /**
         * 分段ID
         */
        private String segmentId;

        /**
         * 教材原文片段
         */
        private String content;

        /**
         * 相似度得分
         */
        private Double score;

        /**
         * Dify 文档ID
         */
        private String documentId;

        /**
         * 文档名称
         */
        private String documentName;

        /**
         * 章节ID（来自文档元数据）
         */
        private Object chapterId;

        /**
         * 页码（来自文档元数据）
         */
        private Integer page;
    }
}
