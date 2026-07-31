package com.amlve.ai.service.impl;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.embeddings.TextEmbeddingResultItem;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.amlve.ai.service.RagService;
import com.amlve.ai.service.RouterLlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    @Value("${ai.alibaba.api-key}")
    private String apiKey;
    private final RouterLlmService routerLlmService;

    List<String> users = List.of("美食", "建筑", "旅行");
    static String query = "汉堡";
    @Override
    public void find() {
        log.info("find {}", query);
        double maxSimilarity = -1.0;
        int index = 0;


        List<double[]> embed = embed(users);
        List<String> q =  new ArrayList<>();
        q.add(query);

        for(int i = 0;i < users.size();i++){
            double v = cosineSimilarity(embed.get(i), embed(q).get(0));
            if(maxSimilarity < v){
                maxSimilarity = v;
                index = i;
            }
            log.info("cur similarity {},{}", v ,  users.get(i));
        }
        log.info("max similarity {},{}", maxSimilarity ,  users.get(index));
    }

    private double cosineSimilarity(double[] vectorA, double[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("向量维度不一致: " + vectorA.length + " vs " + vectorB.length);
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        double denominator = Math.sqrt(normA) * Math.sqrt(normB);

        if (denominator == 0.0) {
            return 0.0;
        }

        return dotProduct / denominator;
    }

    public List<double[]> embed(List<String> texts) {
        // 构建参数（必须指定 textType 以优化检索效果）
        TextEmbeddingParam param = TextEmbeddingParam.builder()
                .apiKey(apiKey)
                .model("text-embedding-v4")
                .texts(texts)
                .dimension(1024)
                .textType(TextEmbeddingParam.TextType.QUERY) // 或 DOCUMENT
                // .instruct("Given a product review, retrieve similar reviews") // 仅当 textType=QUERY 时生效
                .build();

        // 调用模型
        TextEmbedding embedding = new TextEmbedding();
        try {
            TextEmbeddingResult result = embedding.call(param);

            // 提取向量数据（SDK 返回的是 Embedding 对象列表）
            return result.getOutput()
                    .getEmbeddings()
                    .stream()
                    .map(TextEmbeddingResultItem::getEmbedding)
                    .map(embobj -> {
                        double[] emb = new double[embobj.size()];
                        for (int i = 0; i < embobj.size(); i++) {
                            emb[i] = embobj.get(i);
                        }
                        return emb;
                    })
                    .toList();

        } catch (NoApiKeyException e) {
            throw new IllegalArgumentException("API Key 未配置或无效", e);
        } catch (Exception e) {
            throw new RuntimeException("向量化调用失败", e);
        }
    }
}
