package com.amlve.ai.util;


import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmbeddingUtil {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    public TextEmbeddingParam param(List<String> texts) {
        // 构建参数（必须指定 textType 以优化检索效果）
        // 或 DOCUMENT
        // .instruct("Given a product review, retrieve similar reviews") // 仅当 textType=QUERY 时生效
        return TextEmbeddingParam.builder()
                .apiKey(apiKey)
                .model("text-embedding-v4")
                .texts(texts)
                .dimension(1024)
                .textType(TextEmbeddingParam.TextType.QUERY) // 或 DOCUMENT
                // .instruct("Given a product review, retrieve similar reviews") // 仅当 textType=QUERY 时生效
                .build();
    }
}
