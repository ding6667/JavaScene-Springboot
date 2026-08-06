package com.amlve.ai.configuration;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.deepseek.DeepSeekChatModel;

import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {


    @ConditionalOnBean(MilvusVectorStore.class)
    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(MilvusVectorStore milvusVectorStore) {
        ContextualQueryAugmenter contextualQueryAugmenter = ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)
                .build();
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(milvusVectorStore)
                        .topK(3)
                        .similarityThreshold(0.5)
                        .build())
                .queryAugmenter(contextualQueryAugmenter)
                .build();

    }

    @ConditionalOnBean(MilvusVectorStore.class)
    @Bean
    public QuestionAnswerAdvisor questionAnswerAdvisor(MilvusVectorStore milvusVectorStore) {
        return QuestionAnswerAdvisor.builder(milvusVectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.5)
                        .topK(3)
                        .build())
                .build();

    }

    // deepseek 配置
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().build();
    }
    @Bean
    public ChatClient chatClient(DeepSeekChatModel openAiChatModel, ChatMemory chatMemory) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem("你是个测试助手，当用户向你提问时，你要调用工具tool1，并直接返回工具的返回值")
                .defaultAdvisors(new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();

    }

    // embedding 配置, alibaba 云的 embedding 模型
    @Bean
    public TextEmbedding textEmbedding(@Value("${spring.ai.dashscope.api-key}") String apiKey) {
        return new TextEmbedding(apiKey);
    }

}
