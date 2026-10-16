package com.amlve.agentqa.client;

import com.amlve.agentqa.constant.DifyConstant;
import com.amlve.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dify 知识库底层 HTTP 封装（DatasetWebClient）
 * <p>
 * 只负责与 Dify 的原始 HTTP 通信，返回原始 JSON 字符串；
 * 业务语义（如文档ID提取、元数据写入流程）由 {@code QAAgentClientImpl} 完成。
 * <p>
 * 鉴权方式：Authorization: Bearer {知识库API密钥}，由 {@code DifyClientConfig} 统一注入请求头。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatasetWebClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Qualifier("knowledgeDifyWebClient")
    private final WebClient difyWebClient;

    /**
     * 从文本创建文档
     * POST /datasets/{dataset_id}/document/create-by-text
     *
     * @param datasetId 知识库ID
     * @param name      文档名称
     * @param text      文档文本内容
     * @return 原始 JSON 响应（含 document.id 与 batch）
     */
    public String createDocumentByText(String datasetId, String name, String text) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("text", text);
        body.put("indexing_technique", DifyConstant.INDEXING_TECHNIQUE_HIGH_QUALITY);
        body.put("process_rule", Map.of("mode", DifyConstant.PROCESS_MODE_AUTOMATIC));
        return postJson(DifyConstant.CREATE_DOCUMENT_BY_TEXT, datasetId, body);
    }

    /**
     * 从文件创建文档
     * POST /datasets/{dataset_id}/document/create-by-file
     * multipart 表单：data（处理规则 JSON 字符串）+ file（文件）
     *
     * @param datasetId    知识库ID
     * @param file         要上传的文件
     * @param documentName 文档名称（可选，null 则使用文件原始名）
     * @return 原始 JSON 响应（含 document.id 与 batch）
     */
    public String createDocumentByFile(String datasetId, File file, String documentName) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("indexing_technique", DifyConstant.INDEXING_TECHNIQUE_HIGH_QUALITY);
        dataMap.put("process_rule", Map.of("mode", DifyConstant.PROCESS_MODE_AUTOMATIC));
        if (documentName != null && !documentName.isBlank()) {
            dataMap.put("name", documentName);
        }
        String dataJson;
        try {
            dataJson = OBJECT_MAPPER.writeValueAsString(dataMap);
        } catch (IOException e) {
            throw new BusinessException("构建 Dify 文档创建请求失败", e);
        }

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("data", dataJson).header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        builder.part("file", new FileSystemResource(file) {
            @Override
            public String getFilename() {
                return file.getName();
            }
        });
        MultiValueMap<String, org.springframework.http.HttpEntity<?>> parts = builder.build();
        try {
            return difyWebClient.post()
                    .uri(DifyConstant.CREATE_DOCUMENT_BY_FILE, datasetId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(parts))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::toError)
                    .bodyToMono(String.class)
                    .block();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Dify 文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量更新文档元数据
     * POST /datasets/{dataset_id}/documents/metadata
     * <p>
     * 注意：元数据字段需先在知识库中创建（控制台"元数据"或创建元数据字段 API），
     * 否则 Dify 返回 404 metadata not found。
     *
     * @param datasetId  知识库ID
     * @param documentId 文档ID
     * @param metadata   元数据键值对（如 chapter_id -> 3）
     * @return 原始 JSON 响应（result: success）
     */
    public String updateDocumentMetadata(String datasetId, String documentId, Map<String, Object> metadata) {
        Map<String, String> fieldIds = resolveMetadataFieldIds(datasetId, metadata);
        List<Map<String, Object>> metadataList = metadata.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", fieldIds.get(entry.getKey()));
                    item.put("name", entry.getKey());
                    item.put("value", entry.getValue());
                    return item;
                })
                .toList();
        Map<String, Object> operation = new HashMap<>();
        operation.put("document_id", documentId);
        operation.put("metadata_list", metadataList);
        operation.put("partial_update", true);
        Map<String, Object> body = Map.of("operation_data", List.of(operation));
        return postJson(DifyConstant.UPDATE_DOCUMENT_METADATA, datasetId, body);
    }

    /**
     * 解析元数据字段名到字段ID的映射；字段不存在时按值类型自动创建。
     * <p>
     * Dify 的文档元数据更新接口要求 metadata_list 每项携带字段的 id，
     * id 由 Dify 在字段创建时分配，需先查 GET /datasets/{id}/metadata 获取。
     */
    private Map<String, String> resolveMetadataFieldIds(String datasetId, Map<String, Object> metadata) {
        Map<String, String> fieldIds = new HashMap<>();
        String response = getJson(DifyConstant.DATASET_METADATA, datasetId);
        try {
            OBJECT_MAPPER.readTree(response).path("doc_metadata").forEach(node ->
                    fieldIds.put(node.path("name").asText(), node.path("id").asText()));
        } catch (IOException e) {
            throw new BusinessException("解析 Dify 元数据字段列表失败: " + e.getMessage(), e);
        }
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (!fieldIds.containsKey(entry.getKey())) {
                String type = entry.getValue() instanceof Number
                        ? DifyConstant.METADATA_TYPE_NUMBER : DifyConstant.METADATA_TYPE_STRING;
                String created = postJson(DifyConstant.DATASET_METADATA, datasetId,
                        Map.of("name", entry.getKey(), "type", type));
                try {
                    fieldIds.put(entry.getKey(), OBJECT_MAPPER.readTree(created).path("id").asText());
                } catch (IOException e) {
                    throw new BusinessException("解析 Dify 元数据字段创建响应失败: " + e.getMessage(), e);
                }
            }
        }
        return fieldIds;
    }

    /**
     * 从知识库检索分段
     * POST /datasets/{dataset_id}/retrieve
     *
     * @param datasetId      知识库ID
     * @param query          检索关键词
     * @param topK           返回条数（null 则使用默认值）
     * @param scoreThreshold 相似度阈值（null 则使用默认值）
     * @return 原始 JSON 响应（含 records 数组）
     */
    public String retrieve(String datasetId, String query, Integer topK, Double scoreThreshold) {
        int effectiveTopK = topK != null ? topK : DifyConstant.DEFAULT_TOP_K;
        double effectiveThreshold = scoreThreshold != null ? scoreThreshold : DifyConstant.DEFAULT_SCORE_THRESHOLD;
        Map<String, Object> body = Map.of(
                "query", query,
                "retrieval_model", Map.of(
                        "search_method", DifyConstant.SEARCH_METHOD_HYBRID,
                        "reranking_enable", false,
                        "top_k", effectiveTopK,
                        "score_threshold", effectiveThreshold,
                        "score_threshold_enabled", true
                )
        );
        return postJson(DifyConstant.RETRIEVE, datasetId, body);
    }

    /**
     * 查询文档索引状态
     * GET /datasets/{dataset_id}/documents/{batch}/indexing-status
     *
     * @param datasetId 知识库ID
     * @param batch     创建文档响应中的 batch（一次创建的批次号）
     * @return 原始 JSON 响应（data 数组含 indexing_status / error 字段）
     */
    public String getIndexingStatus(String datasetId, String batch) {
        return getJson(DifyConstant.DOCUMENT_INDEXING_STATUS, datasetId, batch);
    }

    /**
     * 删除知识库文档
     * DELETE /datasets/{dataset_id}/documents/{document_id}
     * <p>
     * 404（文档本就不存在）视为删除成功并返回 true，不抛异常——清理动作反过来
     * 打断主流程会让"上传失败"变成"清理失败"，掩盖真实原因。
     *
     * @param datasetId  知识库ID
     * @param documentId 文档ID
     * @return true=已删除或本就无需删除；false=删除请求异常，由调用方记日志
     */
    public boolean deleteDocument(String datasetId, String documentId) {
        try {
            difyWebClient.delete()
                    .uri(DifyConstant.DELETE_DOCUMENT, datasetId, documentId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, response -> Mono.empty())
                    .onStatus(HttpStatusCode::isError, this::toError)
                    .bodyToMono(String.class)
                    .block();
            log.info("已删除 Dify 文档: datasetId={}, documentId={}", datasetId, documentId);
            return true;
        } catch (Exception e) {
            log.error("删除 Dify 文档失败: datasetId={}, documentId={}, error={}",
                    datasetId, documentId, e.getMessage());
            return false;
        }
    }

    /**
     * 统一的 JSON GET 请求，带错误处理
     */
    private String getJson(String uriTemplate, Object... uriVariables) {
        try {
            String response = difyWebClient.get()
                    .uri(uriTemplate, uriVariables)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::toError)
                    .bodyToMono(String.class)
                    .block();
            log.debug("Dify 响应: uri={}, body={}", uriTemplate, response);
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Dify API 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 统一的 JSON POST 请求，带错误处理
     */
    private String postJson(String uriTemplate, String datasetId, Object body) {
        try {
            String response = difyWebClient.post()
                    .uri(uriTemplate, datasetId)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::toError)
                    .bodyToMono(String.class)
                    .block();
            log.debug("Dify 响应: uri={}, body={}", uriTemplate, response);
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Dify API 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将 Dify 的 4xx/5xx 错误响应非阻塞地映射为 BusinessException 信号。
     * <p>
     * 注意：onStatus 回调运行在 Reactor Netty IO 线程上，禁止调用 block()，
     * 必须以响应式链方式读取响应体后再发出 Mono.error。
     */
    private Mono<? extends Throwable> toError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> {
                    log.error("Dify API 错误响应: status={}, body={}", response.statusCode(), body);
                    return Mono.error(new BusinessException(
                            "Dify API 调用失败[" + response.statusCode().value() + "]: " + body));
                });
    }
}
