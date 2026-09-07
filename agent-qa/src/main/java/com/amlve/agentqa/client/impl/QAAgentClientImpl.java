package com.amlve.agentqa.client.impl;

import com.amlve.agentqa.client.DatasetWebClient;
import com.amlve.agentqa.constant.DifyConstant;
import com.amlve.agentqaapi.QAAgentClient;
import com.amlve.agentqaapi.dto.response.DifyRetrieveResponse;
import com.amlve.common.exception.BusinessException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * QAAgentClient 实现类（教材知识答疑智能体客户端）
 * <p>
 * 职责：调用 {@link DatasetWebClient} 与 Dify 知识库通信，并将原始 JSON 解析为
 * agent-qa-api 模块定义的 DTO，供 knowledge 等业务模块使用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QAAgentClientImpl implements QAAgentClient {

    /**
     * 必须忽略未知字段：Dify 检索响应的 segment/document 含大量额外字段
     * （position、word_count、tokens、status 等），DTO 仅声明业务所需字段
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final DatasetWebClient datasetWebClient;

    @Override
    public String createDocumentByText(String datasetId, String documentName, String text) {
        log.info("Dify 创建文本文档: datasetId={}, documentName={}", datasetId, documentName);
        String response = datasetWebClient.createDocumentByText(datasetId, documentName, text);
        String documentId = extractDocumentId(response);
        log.info("Dify 文档创建成功(异步索引中): documentId={}, documentName={}", documentId, documentName);
        return documentId;
    }

    @Override
    public String updateDocumentMetadata(String datasetId, String documentId, Map<String, Object> metadata) {
        log.info("Dify 更新文档元数据: datasetId={}, documentId={}, metadata={}", datasetId, documentId, metadata);
        String response = datasetWebClient.updateDocumentMetadata(datasetId, documentId, metadata);
        log.info("Dify 文档元数据更新完成: documentId={}", documentId);
        return response;
    }

    @Override
    public void createChapterDocument(String datasetId, String documentName, String chapterContent, Integer chapterId) {
        log.info("Dify 上传章节文档: datasetId={}, documentName={}, chapterId={}", datasetId, documentName, chapterId);
        // 1. 先创建文档（内部调用 createDocumentByText）
        String documentId = createDocumentByText(datasetId, documentName, chapterContent);
        // 2. 再写入章节元数据（内部调用 updateDocumentMetadata），便于检索时按章节过滤
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(DifyConstant.METADATA_KEY_CHAPTER_ID, chapterId);
        updateDocumentMetadata(datasetId, documentId, metadata);
        log.info("Dify 章节文档上传完成: documentId={}, chapterId={}", documentId, chapterId);
    }

    @Override
    public String createDocumentByFile(String datasetId, File file, String documentName, Map<String, Object> metadata) {
        log.info("Dify 上传文件文档: datasetId={}, file={}, documentName={}", datasetId, file.getName(), documentName);
        String response = datasetWebClient.createDocumentByFile(datasetId, file, documentName);
        String documentId = extractDocumentId(response);
        // 元数据可选：传入则写入
        if (metadata != null && !metadata.isEmpty()) {
            updateDocumentMetadata(datasetId, documentId, metadata);
        }
        log.info("Dify 文件文档创建成功(异步索引中): documentId={}, file={}", documentId, file.getName());
        return documentId;
    }

    @Override
    public DifyRetrieveResponse retrieve(String datasetId, String query, Integer topK, Double scoreThreshold) {
        log.info("Dify 知识库检索: datasetId={}, query={}, topK={}, scoreThreshold={}", datasetId, query, topK, scoreThreshold);
        String response = datasetWebClient.retrieve(datasetId, query, topK, scoreThreshold);
        DifyRetrieveResponse result = parseRetrieveResponse(response);
        log.info("Dify 检索完成: query={}, 命中记录数={}", query,
                result.getRecords() == null ? 0 : result.getRecords().size());
        return result;
    }

    /**
     * 从创建文档响应中提取 document.id
     */
    private String extractDocumentId(String response) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(response);
            JsonNode documentIdNode = root.path("document").path("id");
            if (documentIdNode.isMissingNode() || documentIdNode.isNull()) {
                throw new BusinessException("Dify 创建文档响应中缺少 document.id: " + response);
            }
            return documentIdNode.asText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("解析 Dify 创建文档响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析检索响应为 DifyRetrieveResponse
     * <p>
     * 注意：Dify 返回的 query 是对象（{"content": "..."}），
     * 而 DifyRetrieveResponse.query 为 String，因此手动解析而非直接反序列化。
     */
    private DifyRetrieveResponse parseRetrieveResponse(String response) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(response);
            DifyRetrieveResponse result = new DifyRetrieveResponse();
            JsonNode queryNode = root.path("query");
            if (queryNode.isObject()) {
                result.setQuery(queryNode.path("content").asText(null));
            } else if (queryNode.isTextual()) {
                result.setQuery(queryNode.asText());
            }

            List<DifyRetrieveResponse.Record> records = new ArrayList<>();
            JsonNode recordsNode = root.path("records");
            if (recordsNode.isArray()) {
                for (JsonNode recordNode : recordsNode) {
                    records.add(OBJECT_MAPPER.convertValue(recordNode, DifyRetrieveResponse.Record.class));
                }
            }
            result.setRecords(records);
            return result;
        } catch (Exception e) {
            throw new BusinessException("解析 Dify 检索响应失败: " + e.getMessage(), e);
        }
    }
}
