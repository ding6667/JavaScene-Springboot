package com.amlve.ai.service.impl;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.embeddings.TextEmbeddingResultItem;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.amlve.ai.entity.EmbdItem;
import com.amlve.ai.service.RagService;
import com.amlve.ai.service.RouterLlmService;
import com.amlve.ai.util.EmbeddingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {


    private final RouterLlmService routerLlmService;
    private final TextEmbedding embedding;
    private final EmbeddingUtil embeddingUtil;
    private final ChatClient chatClient;


    @Override
    public Flux<String> answer(String message) {
        List<String> users = List.of("美食", "建筑", "旅行");

        int topK = 3;
        double threshold = 0.5;

        log.info("answer {}", message);
        List<EmbdItem> similarities = new ArrayList<>();
        List<double[]> embed = embed(users);
        List<String> context = new ArrayList<>();
        for(int i = 0;i < users.size();i++){
            double[] q = embed(message);
            double v = cosineSimilarity(embed.get(i), q);
            if(v > threshold){
                similarities.add(new EmbdItem(i, q));
            }
        }
        similarities.sort((a, b) -> Double.compare(b.getSimilarities()[0], a.getSimilarities()[0]));
        // 取topK个相似度最高的,并添加上下文
        similarities.subList(0, topK).forEach(v -> {
            if(v.getIndex() - 1 >= 0)
                context.add(users.get(v.getIndex() - 1));
            context.add(users.get(v.getIndex()));
            if(v.getIndex() + 1 < users.size())
                context.add(users.get(v.getIndex() + 1));
        });
        String contextStr = String.join("\n", context);
        return chatClient.prompt()
                .user(message + contextStr + "\n")
                .stream()
                .content();

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

        TextEmbeddingParam param = embeddingUtil.param(texts);

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

    public double[] embed(String text) {
        return embed(List.of(text)).get(0);
    }
}
