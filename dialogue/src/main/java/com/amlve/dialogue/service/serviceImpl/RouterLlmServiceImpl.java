package com.amlve.dialogue.service.serviceImpl;

import com.amlve.dialogue.dto.IntentJsonDTO;
import com.amlve.dialogue.enumeration.Intent;
import com.amlve.dialogue.enumeration.TargetScope;
import com.amlve.dialogue.mapper.IntentDraftMapper;
import com.amlve.dialogue.service.AiService;
import com.amlve.dialogue.service.RouterLlmService;
import com.amlve.dialogue.service.tools.AnalyzeTools;
import com.amlve.dialogue.service.tools.ExecuteTools;
import com.amlve.dialogue.service.tools.QueryTools;
import com.amlve.dialogue.service.tools.UnknownIntentTools;
import com.amlve.dialogue.tableEnt.IntentDraft;
import com.amlve.dialogue.utils.ToolsFilterUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouterLlmServiceImpl implements RouterLlmService {

    @Qualifier("intentVectorStore")
    private final MilvusVectorStore intentVectorStore;
    private final AiService aiService;

    private final IntentDraftMapper intentDraftMapper;

    /**
     * 意图识别
     * @param query 用户查询
     * @return 识别出的意图，若无法识别则返回 UNKNOWN
     */
    public Map<String, Object> resolveIntent(String query,String apiKey) {
        List<Document> docs = intentVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(2)
                        .similarityThreshold(0.5)
                        .build()
        );
        log.info("相似度检索结果数: {}, 最高分: {} 第二个L:{}",
                docs.size(),
                docs.isEmpty() ? "无" : docs.getFirst()
                , docs.getLast());

        if (docs.isEmpty()) {
            return Map.of("intent", Intent.UNKNOWN);
        }

        Document best = docs.getFirst();
        Intent bestIntent = Intent.fromCode(best.getMetadata().get("intent_type").toString());
        TargetScope bestTargetScope = TargetScope.fromCode(best.getMetadata().get("target_scope").toString());
        //  高置信度且为 EXECUTE -> 直接返回
        if (best.getScore() > 0.7 && bestIntent == Intent.EXECUTE) {
            return Map.of("intent", bestIntent, "target_scope", bestTargetScope);
        }

        // 若有第二个结果，判断是否需要 AI 仲裁
        if (docs.size() > 1) {

            Document second = docs.get(1);
            double scoreDiff = best.getScore() - second.getScore();
            String secondIntentCode = (String) second.getMetadata().get("intent_type");
            Intent secondIntent = Intent.fromCode(secondIntentCode);

            if (bestIntent == secondIntent) {
                return Map.of("intent", bestIntent, "target_scope", bestTargetScope);
            }
            if (scoreDiff > 0.15) {
                return Map.of("intent", bestIntent, "target_scope", bestTargetScope);
            }
            PromptTemplate promptTemplate = new PromptTemplate(new ClassPathResource("prompt/user/intent.st"));

            String promptText = promptTemplate.render(Map.of("query", query
                    , "template1", Objects.requireNonNull(best.getText())
                    , "intent1", bestIntent.getCode()
                    , "template2", Objects.requireNonNull(second.getText())
                    , "intent2", secondIntentCode));

            BeanOutputConverter<IntentJsonDTO> converter = new BeanOutputConverter<>(IntentJsonDTO.class);
            String contentJson = aiService.createChatClient(apiKey)
                    .prompt()
                    .user(promptText)
                    .call()
                    .content();

            if (contentJson != null) {
                IntentJsonDTO dto = converter.convert(contentJson);
                if (dto.getIsNewIntent()) {
                    saveIntentDraft(dto, query);
                    return Map.of("intent", Intent.NEW_INTENT);
                } else {
                    return Map.of("intent", Intent.fromCode(dto.getIntentType()));
                }
            }
        }
        return Map.of("intent", Intent.UNKNOWN);
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
