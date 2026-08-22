package com.amlve.dialogue.service.serviceImpl;


import com.amlve.common.utils.SecurityUtils;
import com.amlve.dialogueapi.dto.StudentRagInsertDTO;
import com.amlve.dialogue.enumeration.AspectType;
import com.amlve.dialogue.service.RagService;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.springframework.ai.vectorstore.SearchRequest;

import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.ai.document.Document;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagServiceImpl implements RagService ,com.amlve.dialogueapi.service.RagService {

    private static final int BATCH_SIZE = 10;



    @Qualifier("studentResultVectorStore")
    private final MilvusVectorStore studentResultVectorStore;

    @Override
    public List<String> queryContent(String query, List<String> taskIds, String userId, Double maxScore, Double minScore) {
        userId = userId != null ?
                userId :
                SecurityUtils.getUserId().toString();

        FilterExpressionBuilder b = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op filterOp = b.eq("user_id", userId);

        if (taskIds != null && !taskIds.isEmpty()) {
            filterOp = b.and(filterOp, b.in("task_id", (Object[]) taskIds.toArray(new String[0])));
        }

        if (minScore != null) {
            filterOp = b.and(filterOp, b.gte("score", minScore));
        }

        if (maxScore != null) {
            filterOp = b.and(filterOp, b.lte("score", maxScore));
        }

        List<Document> docs = studentResultVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .filterExpression(filterOp.build())
                        .topK(5)
                        .build()
        );

        return docs.stream().map(Document::getText).collect(Collectors.toList());
    }

    @Override
    public void addContent(List<StudentRagInsertDTO> ragInsertDTOs) {
        List<Document> documents = new ArrayList<>();
        for (StudentRagInsertDTO ragInsertDTO : ragInsertDTOs) {
            String id = ragInsertDTO.getTaskId() + ragInsertDTO.getStudentId();

            Map<String, Object> metadata1 = new HashMap<>();
            metadata1.put("id", id);
            metadata1.put("user_id", SecurityUtils.getUserId().toString());
            metadata1.put("task_id", ragInsertDTO.getTaskId());
            metadata1.put("student_id", ragInsertDTO.getStudentId());
            metadata1.put("aspect_type", "总内容");
            metadata1.put("submit_time", System.currentTimeMillis());
            metadata1.put("parent_id", null);
            metadata1.put("semester", ragInsertDTO.getSemester());
            metadata1.put("score", ragInsertDTO.getScore());
            Document document1 = new Document(ragInsertDTO.getText(), metadata1);
            documents.add(document1);

            for (AspectType aspectType : AspectType.values()) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("id", UUID.randomUUID().toString());
                metadata.put("user_id", SecurityUtils.getUserId().toString());
                metadata.put("task_id", ragInsertDTO.getTaskId());
                metadata.put("student_id", ragInsertDTO.getStudentId());
                metadata.put("aspect_type", aspectType.getDesc());
                metadata.put("submit_time", System.currentTimeMillis());
                metadata.put("parent_id", id);
                metadata.put("semester", ragInsertDTO.getSemester());
                metadata.put("score", ragInsertDTO.getScore());
                Document document = new Document(aspectType.extractFrom(ragInsertDTO.getCommentJson()), metadata);
                documents.add(document);
            }

        }

        // DashScope Embedding API 单次最多 10 条，分批导入
        for (int i = 0; i < documents.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, documents.size());
            List<Document> batch = documents.subList(i, end);
            log.info("导入第 {}/{} 批，共 {} 条", (i / BATCH_SIZE) + 1,
                    (documents.size() + BATCH_SIZE - 1) / BATCH_SIZE, batch.size());
            studentResultVectorStore.add(batch);
        }
    }

}
