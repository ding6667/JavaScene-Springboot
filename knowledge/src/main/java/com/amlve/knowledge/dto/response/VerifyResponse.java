package com.amlve.knowledge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 内容校验响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyResponse {

    /**
     * 是否通过（包含至少一个教材关键词）
     */
    private Boolean passed;

    /**
     * 命中的关键词列表
     */
    private List<String> matchedKeywords;

    /**
     * 业务提示文案
     */
    private String message;
}
