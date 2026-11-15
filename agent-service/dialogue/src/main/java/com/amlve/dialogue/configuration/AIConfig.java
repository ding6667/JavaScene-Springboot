package com.amlve.dialogue.configuration;


import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;

import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {


    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(
            @Qualifier("studentResultVectorStore")MilvusVectorStore milvusVectorStore) {
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

    @Bean
    public QuestionAnswerAdvisor questionAnswerAdvisor(
            @Qualifier("studentResultVectorStore")MilvusVectorStore milvusVectorStore) {
        return QuestionAnswerAdvisor.builder(milvusVectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.5)
                        .topK(3)
                        .build())
                .build();

    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().build();
    }


}
