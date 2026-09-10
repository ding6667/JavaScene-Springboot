package com.amlve.knowledgeapi.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class WeightConfigVO {
    private Long id;
    private Long chapterId;
    private Long knowledgePointId;
    private String knowledgePointName;  // 关联查询知识点名称
    private BigDecimal weight;
    private Integer baseQuestionCount;
    private Integer maxQuestionCount;
    private String difficultyDistribution;  // "30,50,20"
    private Boolean isActive;
}