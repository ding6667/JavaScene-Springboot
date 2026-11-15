package com.amlve.agentqaapi.dto.request;

import lombok.Data;


/**
 * Dify 知识库检索请求
 */
@Data
public class DifyRetrieveRequest {
    private String query;
    private RetrievalModel retrievalModel;

    @Data
    public static class RetrievalModel {
        private String searchMethod = "hybrid_search";
        private Boolean rerankingEnable = false;
        private String rerankingMode = "reranking_model";
        private Integer topK = 5;
        private Double scoreThreshold = 0.3;
    }
}