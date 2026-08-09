package com.amlve.ai.service.impl;

import com.amlve.ai.dto.IntentJsonDTO;
import com.amlve.ai.enumeration.Intent;
import com.amlve.ai.mapper.IntentDraftMapper;
import com.amlve.ai.service.RouterLlmService;
import com.amlve.ai.service.tools.*;
import com.amlve.ai.tableEnt.IntentDraft;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouterLlmServiceImpl implements RouterLlmService {
    private final MilvusVectorStore vectorStore;

    @Qualifier("intentChatClient")
    private final ChatClient intentChatClient;
    private final IndividualSearchTools individualSearchTools;
    @Qualifier("commonChatClient")
    private final ChatClient commonChatClient;
    private final IntentDraftMapper intentDraftMapper;
    private final UnknownIntentTools unknownIntentTools;
    private final CommandTools commandTools;

    private final GroupSearchTools groupSearchTools;
    private final CompareTools compareTools;
    private final TrendSearchTools trendSearchTools;
    /**
     * 意图识别（不含副作用）
     * @param query 用户查询
     * @return 识别出的意图，若无法识别则返回 UNKNOWN
     */
    public Intent resolveIntent(String query) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(2)
                        .similarityThreshold(0.5)
                        .build()
        );

        if (docs.isEmpty()) {
            return Intent.UNKNOWN;
        }

        Document best = docs.get(0);
        String bestIntentCode = (String) best.getMetadata().get("intent_type");
        Intent bestIntent = Intent.fromCode(bestIntentCode);

        //  高置信度且为 COMMAND -> 直接返回
        if (best.getScore() > 0.7 && bestIntent == Intent.COMMAND) {
            return Intent.COMMAND;
        }

        // 若有第二个结果，判断是否需要 AI 仲裁
        if (docs.size() > 1) {
            Document second = docs.get(1);
            double scoreDiff = best.getScore() - second.getScore();
            String secondIntentCode = (String) second.getMetadata().get("intent_type");
            Intent secondIntent = Intent.fromCode(secondIntentCode);

            // 分数差距 > 0.15 → 信任第一
            if (scoreDiff > 0.15) {
                return bestIntent;
            }

            // 分数接近且意图不同，让 AI 仲裁
            if (bestIntent != secondIntent) {
                BeanOutputConverter<IntentJsonDTO> converter = new BeanOutputConverter<>(IntentJsonDTO.class);
                String contentJson = intentChatClient.prompt()
                        .user("""
                                用户问题：%s
                                现在匹配到了两个最相似的模板：
                                模板1: %s (意图: %s)
                                模板2: %s (意图: %s)
                                请根据用户问题关键词判断用户意图，若都不匹配则标记为新意图。
                                """.formatted(query,
                                best.getText(), bestIntentCode,
                                second.getText(), secondIntentCode)
                                + converter.getFormat())
                        .call()
                        .content();

                if (contentJson != null) {
                    IntentJsonDTO dto = converter.convert(contentJson);
                    if (dto.getIsNewIntent()) {
                        saveIntentDraft(dto, query);
                        return Intent.NEW_INTENT;
                    } else {
                        return Intent.fromCode(dto.getIntentType());
                    }
                }
            }
        }
        return bestIntent;
    }

    /**
     * 根据意图返回对应的工具回调列表
     * @param intent 识别出的意图
     * @return 工具回调列表（可能为空）
     */
    public List<ToolCallback> getToolsForIntent(Intent intent) {
        if (intent == null || intent == Intent.UNKNOWN) {
            return List.of(ToolCallbacks.from(unknownIntentTools));
        }

        // 根据意图返回不同的工具
        switch (intent) {
            case INDIVIDUAL:
                return List.of(ToolCallbacks.from(individualSearchTools)); // 假设是查询个体的工具
            case GROUP:
                return List.of(ToolCallbacks.from(groupSearchTools));      // 查询群体的工具
            case TREND:
                return List.of(ToolCallbacks.from(trendSearchTools));      // 趋势分析工具
            case COMPARE:
                return List.of(ToolCallbacks.from(compareTools));          // 对比工具
            case COMMAND:
                return List.of(ToolCallbacks.from(commandTools));          // 数据修改工具
            case NEW_INTENT:
                return List.of(ToolCallbacks.from(unknownIntentTools));
            //TODO 新意图需要特殊处理
            default:
                return List.of();
        }
    }

    private void saveIntentDraft(IntentJsonDTO intentJsonDTO, String query) {
        IntentDraft intentDraft = IntentDraft.builder()
                .draftIntentType(intentJsonDTO.getIntentType())
                .draftTemplateText(intentJsonDTO.getSuggestedTemplates())
                .originalUserQuery(query)
                .status(0)
                .createdTime(LocalDateTime.now())
                .build();
        intentDraftMapper.insert(intentDraft);
    }

}
