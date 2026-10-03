package com.amlve.agentqa.client;

import com.amlve.agentqa.bean.DifyClient;
import com.amlve.agentqa.constant.DifyConstant;
import com.amlve.agentqaapi.service.QAAgentClient;
import com.amlve.agentqaapi.dto.response.DifyRetrieveResponse;
import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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

    /**
     * deepseek 推理模型会在正文前输出 &lt;think&gt;...&lt;/think&gt; 思考链
     * （实测 Dify 工作流的 outputs.text 中带有该块，含 dify-deepseek-reasoning 标记），
     * 不能直接返回给学生；未闭合时视为全部是思考内容一并剥除
     */
    private static final Pattern THINK_BLOCK = Pattern.compile("(?s)<think>.*?(</think>|$)");

    /**
     * 索引状态轮询间隔（毫秒）：Dify 建档后异步索引，通常数秒至数十秒完成
     */
    private static final long INDEXING_POLL_INTERVAL_MS = 3000L;

    /**
     * 索引状态轮询最大次数：约 60 秒，超时视为仍在索引（不判失败，避免误伤慢文档）
     */
    private static final int INDEXING_POLL_MAX_ATTEMPTS = 20;

    private final DatasetWebClient datasetWebClient;

    private final DifyClient difyClient;

    /**
     * 答疑工作流应用密钥（app- 开头）：优先读 dify.workflow-api-key，
     * 未配置时回落到 dify.api-key（需与队长确认工作流应用的实际密钥）
     */
    @Value("${dify.qa-api-key}")
    private String qaApiKey;

    @Override
    public Result<String> createDocumentByText(String datasetId, String documentName, String text) {
        log.info("Dify 创建文本文档: datasetId={}, documentName={}", datasetId, documentName);
        String response = datasetWebClient.createDocumentByText(datasetId, documentName, text);
        String documentId = extractDocumentId(response);
        // 等待索引完成：索引失败（如嵌入模型不可用）时抛异常，调用方感知失败而非假成功
        waitForIndexing(datasetId, extractBatch(response), documentId);
        log.info("Dify 文档创建成功: documentId={}, documentName={}", documentId, documentName);
        return Result.success(documentId);
    }

    @Override
    public Result<String> updateDocumentMetadata(String datasetId, String documentId, Map<String, Object> metadata) {
        log.info("Dify 更新文档元数据: datasetId={}, documentId={}, metadata={}", datasetId, documentId, metadata);
        String response = datasetWebClient.updateDocumentMetadata(datasetId, documentId, metadata);
        log.info("Dify 文档元数据更新完成: documentId={}", documentId);
        return Result.success(response);
    }

    @Override
    public Result<Void> createChapterDocument(String datasetId, String documentName, String chapterContent, Integer chapterId) {
        log.info("Dify 上传章节文档: datasetId={}, documentName={}, chapterId={}", datasetId, documentName, chapterId);
        // 1. 先创建文档（内部调用 createDocumentByText，失败时其内部已抛业务异常）
        String documentId = unwrapInternal(createDocumentByText(datasetId, documentName, chapterContent));
        // 2. 再写入章节元数据（内部调用 updateDocumentMetadata），便于检索时按章节过滤
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(DifyConstant.METADATA_KEY_CHAPTER_ID, chapterId);
        updateDocumentMetadata(datasetId, documentId, metadata);
        log.info("Dify 章节文档上传完成: documentId={}, chapterId={}", documentId, chapterId);
        return Result.success(null);
    }

    @Override
    public Result<String> createDocumentByFile(String datasetId, File file, String documentName, Map<String, Object> metadata) {
        log.info("Dify 上传文件文档: datasetId={}, file={}, documentName={}", datasetId, file.getName(), documentName);
        String response = datasetWebClient.createDocumentByFile(datasetId, file, documentName);
        String documentId = extractDocumentId(response);
        // 元数据可选：传入则写入
        if (metadata != null && !metadata.isEmpty()) {
            updateDocumentMetadata(datasetId, documentId, metadata);
        }
        // 等待索引完成，失败时抛异常，避免接口返回成功但知识库内是空文档
        waitForIndexing(datasetId, extractBatch(response), documentId);
        log.info("Dify 文件文档创建成功: documentId={}, file={}", documentId, file.getName());
        return Result.success(documentId);
    }

    @Override
    public Result<DifyRetrieveResponse> retrieve(String datasetId, String query, Integer topK, Double scoreThreshold) {
        log.info("Dify 知识库检索: datasetId={}, query={}, topK={}, scoreThreshold={}", datasetId, query, topK, scoreThreshold);
        String response = datasetWebClient.retrieve(datasetId, query, topK, scoreThreshold);
        DifyRetrieveResponse result = parseRetrieveResponse(response);
        log.info("Dify 检索完成: query={}, 命中记录数={}", query,
                result.getRecords() == null ? 0 : result.getRecords().size());
        return Result.success(result);
    }

    @Override
    public Result<String> answer(String chapterId, String userQuery) {
        log.info("Dify 答疑工作流: chapterId={}, userQuery={}", chapterId, userQuery);
        if (qaApiKey == null || qaApiKey.isBlank()) {
            throw new BusinessException("Dify 工作流 API 密钥未配置（dify.qa-api-key 或 dify.api-key）");
        }
        Map<String, Object> inputs = buildWorkflowInputs(chapterId, userQuery);
        String response = difyClient.runWorkflowBlocking(qaApiKey, inputs);
        String answerText = extractAnswer(response);
        log.info("Dify 答疑完成: chapterId={}, 答案长度={}", chapterId, answerText.length());
        return Result.success(answerText);
    }

    @Override
    public Flux<String> answerStream(String chapterId, String userQuery) {
        log.info("Dify 答疑工作流(流式): chapterId={}, userQuery={}", chapterId, userQuery);
        if (qaApiKey == null || qaApiKey.isBlank()) {
            return Flux.error(new BusinessException(
                    "Dify 工作流 API 密钥未配置（dify.qa-api-key 或 dify.api-key）"));
        }
        Map<String, Object> inputs = buildWorkflowInputs(chapterId, userQuery);
        return difyClient.streamWorkflow(qaApiKey, inputs)
                // 每个 SSE 事件解析出 0..n 个文本片段；think 与正文的分流由控制层负责
                .flatMapIterable(this::extractStreamTextChunks);
    }

    /**
     * 构造答疑工作流 inputs：chapterId 为数字型字符串时转数值传（Dify 元数据 chapter_id
     * 为 number 类型，开始节点变量声明为数字时要求可转换的类型），非数字或空则不传该参数
     */
    private Map<String, Object> buildWorkflowInputs(String chapterId, String userQuery) {
        Map<String, Object> inputs = new HashMap<>();
        if (chapterId != null && !chapterId.isBlank()) {
            Object chapterIdValue = chapterId.trim();
            try {
                chapterIdValue = Long.valueOf(chapterId.trim());
            } catch (NumberFormatException ignored) {
                // 非数字章节ID按原样传字符串
            }
            inputs.put("chapterId", chapterIdValue);
        }
        inputs.put("userQuery", userQuery);
        return inputs;
    }

    /**
     * 解析单个 Dify 流式事件，提取答案文本片段。
     * <p>
     * 事件 data 为 JSON：{"event": "text_chunk", "data": {"text": "..."}}；
     * workflow_finished 事件含最终 status，失败时以流错误信号终止。
     *
     * @return 文本片段列表（非 text_chunk 事件返回空列表）
     */
    private List<String> extractStreamTextChunks(ServerSentEvent<String> event) {
        String data = event.data();
        if (data == null || data.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(data);
            String type = root.path("event").asText("");
            switch (type) {
                case "text_chunk" -> {
                    String text = root.path("data").path("text").asText("");
                    return text.isEmpty() ? List.of() : List.of(text);
                }
                case "workflow_finished" -> {
                    String status = root.path("data").path("status").asText("");
                    if (!"succeeded".equals(status)) {
                        String error = root.path("data").path("error").asText("未知错误");
                        log.error("Dify 流式工作流执行失败: status={}, error={}", status, error);
                        throw new BusinessException("Dify 工作流执行失败: " + error);
                    }
                    return List.of();
                }
                case "error" -> {
                    String message = root.path("data").path("message").asText("未知错误");
                    log.error("Dify 流式事件返回错误: {}", message);
                    throw new BusinessException("Dify 流式答疑失败: " + message);
                }
                default -> {
                    return List.of();
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("解析 Dify 流式事件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从工作流 blocking 响应中提取答案
     * <p>
     * 响应结构：{ data: { status, outputs: {...}, error } }。
     * outputs 的答案字段名取决于工作流结束节点的输出变量，
     * 优先取常见命名（answer/text/result），未命中时返回原始 outputs JSON 便于联调排查。
     */
    private String extractAnswer(String response) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(response);
            JsonNode dataNode = root.path("data");
            String status = dataNode.path("status").asText("");
            if (!"succeeded".equals(status)) {
                String error = dataNode.path("error").asText("未知错误");
                throw new BusinessException("Dify 工作流执行失败: status=" + status + ", error=" + error);
            }
            JsonNode outputs = dataNode.path("outputs");
            if (outputs.isMissingNode() || outputs.isNull()) {
                throw new BusinessException("Dify 工作流响应缺少 outputs: " + response);
            }
            for (String key : new String[]{"answer", "text", "result"}) {
                JsonNode value = outputs.path(key);
                if (!value.isMissingNode() && !value.isNull()) {
                    return stripReasoning(value.asText());
                }
            }
            log.warn("Dify 工作流 outputs 未含常见答案字段(answer/text/result), 返回原始 outputs: {}", outputs);
            return outputs.toString();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("解析 Dify 工作流响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 剥除模型输出中的思考链块，只保留正式回答
     */
    private String stripReasoning(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return THINK_BLOCK.matcher(text).replaceAll("").trim();
    }

    /**
     * 解包本类其它方法返回的 Result（同模块内部调用）：
     * 失败码时转为业务异常，避免调用链上层拿到 null 后继续执行成"假成功"
     */
    private <T> T unwrapInternal(Result<T> result) {
        if (result == null || result.getCode() != ErrorCodeConstant.SUCCESS) {
            String message = result == null ? "无响应" : result.getMessage();
            log.error("Dify 调用链内部失败: {}", message);
            throw new BusinessException("Dify 知识库调用失败: " + message);
        }
        return result.getData();
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
     * 从创建文档响应中提取 batch（索引状态查询的批次号）
     *
     * @return batch 值；响应缺失时返回 null（跳过索引校验，不影响主流程）
     */
    private String extractBatch(String response) {
        try {
            JsonNode batchNode = OBJECT_MAPPER.readTree(response).path("batch");
            return batchNode.isMissingNode() || batchNode.isNull() ? null : batchNode.asText();
        } catch (Exception e) {
            log.warn("解析 Dify 创建文档响应中的 batch 失败，跳过索引校验", e);
            return null;
        }
    }

    /**
     * 轮询 Dify 文档索引状态直至完成或失败。
     * <p>
     * Dify 建档接口立即返回（此时文档 queuing / 异步索引中），若嵌入模型不可用等原因
     * 导致索引失败，文档会停留在 error 状态且无任何分段——接口若此时返回成功，
     * 调用方拿到的是"假成功"（空文档）。故建档后轮询 indexing-status：
     * <ul>
     *     <li>completed：索引完成，正常返回</li>
     *     <li>error：抛 BusinessException，由调用方事务回滚（MySQL 不落空文档）</li>
     *     <li>超时（约 60 秒仍在索引）：不判失败，仅告警——慢文档 ≠ 失败</li>
     * </ul>
     *
     * @param datasetId  知识库ID
     * @param batch      创建响应中的批次号（null 则跳过校验）
     * @param documentId 文档ID（仅用于日志）
     */
    private void waitForIndexing(String datasetId, String batch, String documentId) {
        if (batch == null || batch.isBlank()) {
            log.warn("缺少 batch，跳过索引状态校验: datasetId={}, documentId={}", datasetId, documentId);
            return;
        }
        for (int attempt = 1; attempt <= INDEXING_POLL_MAX_ATTEMPTS; attempt++) {
            String response = datasetWebClient.getIndexingStatus(datasetId, batch);
            String status = parseIndexingStatus(response);
            switch (status) {
                case "completed" -> {
                    log.info("Dify 文档索引完成: documentId={}, 轮询次数={}", documentId, attempt);
                    return;
                }
                case "error" -> {
                    String error = parseIndexingError(response);
                    log.error("Dify 文档索引失败: documentId={}, error={}", documentId, error);
                    throw new BusinessException("文档索引失败，知识库未收录该文档: " + error);
                }
                default -> log.debug("Dify 文档索引中: documentId={}, status={}, 轮询次数={}",
                        documentId, status, attempt);
            }
            try {
                Thread.sleep(INDEXING_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException("索引状态轮询被中断: documentId=" + documentId, e);
            }
        }
        log.warn("Dify 文档索引超时未完成（不判失败）: documentId={}, batch={}", documentId, batch);
    }

    /**
     * 解析索引状态响应中 data[0].indexing_status
     */
    private String parseIndexingStatus(String response) {
        try {
            return OBJECT_MAPPER.readTree(response).path("data").path(0).path("indexing_status").asText("");
        } catch (Exception e) {
            throw new BusinessException("解析 Dify 索引状态响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析索引状态响应中 data[0].error（索引失败原因，如嵌入模型不可用）
     */
    private String parseIndexingError(String response) {
        try {
            String error = OBJECT_MAPPER.readTree(response).path("data").path(0).path("error").asText("");
            return error.isBlank() ? "未知原因" : error;
        } catch (Exception e) {
            return "未知原因（解析失败: " + e.getMessage() + "）";
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
