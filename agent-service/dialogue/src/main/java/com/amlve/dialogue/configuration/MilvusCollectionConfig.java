package com.amlve.dialogue.configuration;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 多集合 Milvus 配置
 * 为不同业务场景创建独立的 MilvusVectorStore Bean
 */
@Configuration
public class MilvusCollectionConfig {

    @Value("${spring.ai.vectorstore.milvus.collectionNames.studentResult}")
    private String studentResultCollectionName ;
    @Value("${spring.ai.vectorstore.milvus.collectionNames.intention}")
    private String intentionCollectionName ;

    @Value("${spring.ai.vectorstore.milvus.client.username:}")
    private String username;
    @Value("${spring.ai.vectorstore.milvus.client.password:}")
    private String password;
    @Value("${spring.ai.vectorstore.milvus.client.host:localhost}")
    private String host;
    @Value("${spring.ai.vectorstore.milvus.client.port:19530}")
    private int port;
    @Value("${spring.ai.vectorstore.milvus.embeddingDimension:1024}")
    private int embeddingDimension;
    @Value("${spring.ai.vectorstore.milvus.indexType:IVF_FLAT}")
    private String indexType;
    @Value("${spring.ai.vectorstore.milvus.metricType:COSINE}")
    private String metricType;

    /**
     * 意图识别集合 - 用于 RouterLlmServiceImpl
     * 存储意图模板，用于相似度匹配识别用户意图
     */
    @Bean("intentVectorStore")
    public MilvusVectorStore intentVectorStore(@Qualifier("dashscopeEmbeddingModel") EmbeddingModel embeddingModel, MilvusServiceClient milvusServiceClient) {
        return MilvusVectorStore.builder(milvusServiceClient, embeddingModel)
                .collectionName(intentionCollectionName)
                .embeddingDimension(embeddingDimension)
                .indexType(IndexType.valueOf(indexType))
                .metricType(MetricType.valueOf(metricType))
                .build();
    }

    /**
     * 通用集合 - 用于 AIConfig 中的 Advisor
     * 复用已有的 javascene 集合
     */
    @Bean("studentResultVectorStore")
    public MilvusVectorStore studentResultVectorStore(@Qualifier("dashscopeEmbeddingModel") EmbeddingModel embeddingModel, MilvusServiceClient milvusServiceClient) {
        return MilvusVectorStore.builder(milvusServiceClient, embeddingModel)
                .collectionName(studentResultCollectionName)
                .embeddingDimension(embeddingDimension)
                .indexType(IndexType.valueOf(indexType))
                .metricType(MetricType.valueOf(metricType))
                .build();
    }
}