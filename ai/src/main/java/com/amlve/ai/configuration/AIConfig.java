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

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {


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

    @Bean
    public QuestionAnswerAdvisor questionAnswerAdvisor(MilvusVectorStore milvusVectorStore) {
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
    @Bean("specialChatClient")
    public ChatClient specialChatClient(DeepSeekChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultSystem("你是一个专业的学生助手，你的任务是调用工具来回答学生的问题。")
                .defaultAdvisors(new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();

    }

    @Bean("commonChatClient")
    public ChatClient commonChatClient(DeepSeekChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一个友好的教学系统助手。用户刚才的问题没有命中具体的业务查询（如查分、改分、查错）。
                                    请按以下规则回复：
                                    1. 如果用户在闲聊（如问好、问天气），请正常友好回应。
                                    2. 如果用户问题模糊但疑似与学习相关（如“帮我看看作业”），请礼貌地引导用户提供【学号】或【作业名称】，以便系统进一步帮助。
                                    3. 绝对不要编造任何学生的分数或信息。""")
                .defaultAdvisors(new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
    @Bean("intentChatClient")
    public ChatClient intentChatClient(DeepSeekChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        #你是一个专业的意图识别专家，根据用户问题关键词和匹配到的模板意图判断用户意图。
                        #要求
                        1. 你只能根据用户问题关键词和匹配到的模板意图判断用户意图。不可以猜测其他意图。
                        2. 严格按照规定json字段格式回复：
                             Boolean isNewIntent; 如果用户意图和提供的模板意图不匹配，设置为true。否则为false。
                             String intentType; 当isNewIntent为false时，设置为匹配到的模板意图。必须是提供的模板意图中的一个。
                             String suggestedType; 当isNewIntent为true时，设置为建议的意图类型名。
                             List<String> suggestedTemplates; 当isNewIntent为true时，设置为建议的模板。五个以上。
                             String reason; 建议的意图类型名的解释。
                        #提醒
                        1. 如果isNewIntent为true，intentType必须为空。
                        2. 如果isNewIntent为false，suggestedType，suggestedTemplates必须为空。
                        """)

                .defaultAdvisors(new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
