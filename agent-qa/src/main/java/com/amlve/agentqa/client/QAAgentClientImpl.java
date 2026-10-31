package com.amlve.agentqa.client;

import com.amlve.common.constant.ErrorMessageConstant;
import com.amlve.agentqa.bean.DifyClient;
import com.amlve.agentqa.constant.DifyConstant;
import com.amlve.agentqaapi.service.QAAgentClient;
import com.amlve.agentqaapi.dto.response.DifyDocumentVO;
import com.amlve.agentqaapi.dto.response.DifyRetrieveResponse;
import com.amlve.agentqaapi.dto.response.QaWorkflowAnswerVO;
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
     * 索引状态轮询间隔（毫秒）：太短浪费请求数，太长拖慢上传响应
     */
    private static final long INDEXING_POLL_INTERVAL_MS = 2000L;

    /**
     * 索引最长等待时间（毫秒）：大文件解析较慢，5 分钟仍无结果按失败处理
     */
    private static final long INDEXING_MAX_WAIT_MS = 5 * 60 * 1000L;

    /**
     * 索引状态：完成
     */
    private static final String INDEXING_COMPLETED = "completed";

    /**
     * 索引状态：失败
     */
    private static final String INDEXING_ERROR = "error";

    /**
     * 连续多少次都解析不出可用状态就直接判失败清理（说明响应结构与预期不符，
     * 继续轮询只是白占数据库连接）
     */
    private static final int INDEXING_UNKNOWN_STATUS_LIMIT = 5;

    private final DatasetWebClient datasetWebClient;

    private final DifyClient difyClient;

    /**
     * 答疑工作流应用密钥（app- 开头）：优先读 dify.workflow-api-key，
     * 未配置时回落到 dify.api-key（需与队长确认工作流应用的实际密钥）
     */
    @Value("${dify.qa-api-key}")
    private String qaApiKey;

    @Override
    public Result<DifyDocumentVO> createDocumentByText(String datasetId, String documentName, String text) {
        log.info("Dify 创建文本文档: datasetId={}, documentName={}", datasetId, documentName);
        String response = datasetWebClient.createDocumentByText(datasetId, documentName, text);
        String documentId = extractDocumentId(response);
        String batch = extractBatch(response);
        IndexingOutcome outcome = verifyIndexed(datasetId, batch, documentId);
        log.info("Dify 文档创建成功: documentId={}, documentName={}, totalSegments={}",
                documentId, documentName, outcome.totalSegments());
        return Result.success(new DifyDocumentVO(documentId, batch,
                outcome.totalSegments(), outcome.completedSegments()));
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
        // 1. 先创建文档（内部调用 createDocumentByText，校验不通过时其内部已清理并抛业务异常）
        String documentId = unwrapInternal(createDocumentByText(datasetId, documentName, chapterContent))
                .getDocumentId();
        // 2. 再写入章节元数据（内部调用 updateDocumentMetadata），便于检索时按章节过滤
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(DifyConstant.METADATA_KEY_CHAPTER_ID, chapterId);
        updateDocumentMetadata(datasetId, documentId, metadata);
        log.info("Dify 章节文档上传完成: documentId={}, chapterId={}", documentId, chapterId);
        return Result.success(null);
    }

    @Override
    public Result<DifyDocumentVO> createDocumentByFile(String datasetId, File file, String documentName, Map<String, Object> metadata) {
        log.info("Dify 上传文件文档: datasetId={}, file={}, documentName={}", datasetId, file.getName(), documentName);
        String response = datasetWebClient.createDocumentByFile(datasetId, file, documentName);
        String documentId = extractDocumentId(response);
        String batch = extractBatch(response);
        // 元数据可选：传入则写入
        if (metadata != null && !metadata.isEmpty()) {
            updateDocumentMetadata(datasetId, documentId, metadata);
        }
        IndexingOutcome outcome = verifyIndexed(datasetId, batch, documentId);
        log.info("Dify 文件文档创建成功: documentId={}, file={}, totalSegments={}",
                documentId, file.getName(), outcome.totalSegments());
        return Result.success(new DifyDocumentVO(documentId, batch,
                outcome.totalSegments(), outcome.completedSegments()));
    }

    @Override
    public Result<Void> deleteDocument(String datasetId, String documentId) {
        log.info("删除知识库文档: datasetId={}, documentId={}", datasetId, documentId);
        datasetWebClient.deleteDocument(datasetId, documentId);
        return Result.success(null);
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
    public Result<QaWorkflowAnswerVO> answer(String chapterId, String userQuery) {
        log.info("Dify 答疑工作流: chapterId={}, userQuery={}", chapterId, userQuery);
        if (qaApiKey == null || qaApiKey.isBlank()) {
            throw new BusinessException(ErrorMessageConstant.AGENT_UNAVAILABLE);
        }
        Map<String, Object> inputs = buildWorkflowInputs(chapterId, userQuery);
        String response = difyClient.runWorkflowBlocking(qaApiKey, inputs);
        QaWorkflowAnswerVO result = extractWorkflowAnswer(response);
        log.info("Dify 答疑完成: chapterId={}, 答案长度={}, tokens={}",
                chapterId, result.getAnswer().length(), result.getTokensUsed());
        return Result.success(result);
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
                        throw new BusinessException(ErrorMessageConstant.AGENT_UNAVAILABLE);
                    }
                    return List.of();
                }
                case "error" -> {
                    String message = root.path("data").path("message").asText("未知错误");
                    log.error("Dify 流式事件返回错误: {}", message);
                    throw new BusinessException(ErrorMessageConstant.AGENT_UNAVAILABLE);
                }
                default -> {
                    return List.of();
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorMessageConstant.AGENT_RESULT_UNREADABLE, e);
        }
    }

    /**
     * 从工作流 blocking 响应中提取答案与执行元数据
     * <p>
     * 响应结构：{ data: { id, status, outputs:{...}, elapsed_time, total_tokens, error } }。
     * outputs 的答案字段名取决于工作流结束节点的输出变量，
     * 优先取常见命名（answer/text/result），未命中时返回原始 outputs JSON 便于联调排查。
     * <p>
     * 三个元数据字段是 qa_record 里 dify_message_id / tokens_used / response_time 的来源，
     * 取不到就落 null 并记 warn（任务书 6.18.1 明确禁止写假数据）：
     * dify_message_id 取 data.id（工作流执行ID，不是 chat 的 message_id，本应用是 workflow 型），
     * tokens_used 取 data.total_tokens，response_time 由秒为单位的 elapsed_time 折算毫秒。
     */
    private QaWorkflowAnswerVO extractWorkflowAnswer(String response) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(response);
            JsonNode dataNode = root.path("data");
            String status = dataNode.path("status").asText("");
            if (!"succeeded".equals(status)) {
                String error = dataNode.path("error").asText("未知错误");
                throw new BusinessException(ErrorMessageConstant.AGENT_UNAVAILABLE);
            }
            JsonNode outputs = dataNode.path("outputs");
            if (outputs.isMissingNode() || outputs.isNull()) {
                throw new BusinessException(ErrorMessageConstant.AGENT_RESULT_UNREADABLE);
            }
            String answer = null;
            for (String key : new String[]{"answer", "text", "result"}) {
                JsonNode value = outputs.path(key);
                if (!value.isMissingNode() && !value.isNull()) {
                    answer = stripReasoning(value.asText());
                    break;
                }
            }
            if (answer == null) {
                log.warn("Dify 工作流 outputs 未含常见答案字段(answer/text/result), 按原始 outputs 返回: {}", outputs);
                answer = outputs.toString();
            }

            String executionId = firstNonBlank(dataNode.path("id").asText(null),
                    root.path("workflow_run_id").asText(null));
            Integer totalTokens = readInt(dataNode.path("total_tokens"));
            Double elapsedSeconds = readDouble(dataNode.path("elapsed_time"));
            Integer elapsedMs = elapsedSeconds == null ? null : (int) Math.round(elapsedSeconds * 1000);
            if (executionId == null || totalTokens == null) {
                log.warn("Dify 工作流响应缺少执行标识或 token 统计，qa_record 对应列落 null: "
                        + "difyMessageId={}, tokensUsed={}", executionId, totalTokens);
            }
            return new QaWorkflowAnswerVO(answer, executionId, totalTokens, elapsedMs);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorMessageConstant.AGENT_RESULT_UNREADABLE, e);
        }
    }

    /**
     * 数值字段读取：缺失或类型不符返回 null，不用 0 冒充"消耗了 0 token"
     */
    private Integer readInt(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() || !node.isNumber() ? null : node.asInt();
    }

    private Double readDouble(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() || !node.isNumber() ? null : node.asDouble();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null || second.isBlank() ? null : second;
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
            throw new BusinessException(ErrorMessageConstant.KNOWLEDGE_BASE_UNAVAILABLE);
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
                throw new BusinessException(ErrorMessageConstant.KNOWLEDGE_BASE_UNAVAILABLE);
            }
            return documentIdNode.asText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorMessageConstant.KNOWLEDGE_BASE_UNAVAILABLE, e);
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
     * 轮询 Dify 索引状态并校验文档是否真正可用，失败时清理异常文档。
     * <p>
     * Dify 建档接口立即返回（文档处于 queuing / 异步索引中），此时接口返回成功并不代表
     * 知识库里真的有内容。九月测试暴露的"上传成功但内容为空"就属于这类假成功，
     * 因此建档后必须轮询 indexing-status 并按四种结局处理：
     * <ul>
     *     <li>completed 且 total_segments &gt; 0：成功，返回分段数</li>
     *     <li>completed 但 total_segments = 0：空壳，判失败</li>
     *     <li>error：索引失败（如嵌入模型不可用）</li>
     *     <li>超时仍未完成：按失败处理，避免调用方无限等待</li>
     * </ul>
     * 三种失败路径都会先删除 Dify 侧文档再抛业务异常：留下一个空壳文档会让后续检索
     * 命中无意义结果，且占用知识库配额。抛异常同时让 knowledge 侧事务回滚，
     * 本地 knowledge_content 不会留下指向空文档的记录。
     *
     * @param datasetId  知识库ID
     * @param batch      创建响应中的批次号（为空即无法确认结果，按失败清理）
     * @param documentId 文档ID（清理与日志用）
     * @return 索引完成后的状态结论（含总分段数与已完成分段数）
     */
    private IndexingOutcome verifyIndexed(String datasetId, String batch, String documentId) {
        if (batch == null || batch.isBlank()) {
            // 拿不到 batch 就无法确认索引结果，按失败处理并清理：
            // 若放行，knowledge 侧会写出一条 status=成功 且 total_segments=0 的自相矛盾记录，
            // 正是任务 2 要根除的"假成功"后门
            log.error("未取得 batch，无法校验索引结果: datasetId={}, documentId={}", datasetId, documentId);
            throw cleanupAndFail(datasetId, documentId, "未取得 batch，无法确认索引结果");
        }
        long deadline = System.currentTimeMillis() + INDEXING_MAX_WAIT_MS;
        int unknownStatusRounds = 0;
        while (true) {
            if (System.currentTimeMillis() > deadline) {
                log.error("Dify 文档索引超时未完成: documentId={}, batch={}", documentId, batch);
                throw cleanupAndFail(datasetId, documentId, "索引超时未完成");
            }
            IndexingOutcome outcome;
            try {
                outcome = fetchIndexingOutcome(datasetId, batch, documentId);
            } catch (Exception e) {
                log.error("查询 Dify 索引状态失败: documentId={}, err={}", documentId, e.getMessage());
                throw cleanupAndFail(datasetId, documentId, "查询索引状态失败: " + e.getMessage());
            }

            if (outcome == null) {
                // 连续多次拿不到可解析的状态说明响应结构变了，不该白等到超时
                if (++unknownStatusRounds >= INDEXING_UNKNOWN_STATUS_LIMIT) {
                    log.error("连续 {} 次无法解析索引状态: documentId={}", unknownStatusRounds, documentId);
                    throw cleanupAndFail(datasetId, documentId, "索引状态无法识别");
                }
                log.debug("索引状态尚未就绪，继续等待: documentId={}", documentId);
            } else if (INDEXING_COMPLETED.equals(outcome.status())) {
                if (outcome.totalSegments() <= 0) {
                    log.error("文档索引完成但分段为 0，视为空壳: documentId={}", documentId);
                    throw cleanupAndFail(datasetId, documentId, "文档解析为空，未生成任何分段");
                }
                log.info("Dify 文档索引成功: documentId={}, totalSegments={}, completedSegments={}",
                        documentId, outcome.totalSegments(), outcome.completedSegments());
                return outcome;
            } else if (INDEXING_ERROR.equals(outcome.status())) {
                log.error("Dify 文档索引失败: documentId={}, error={}", documentId, outcome.error());
                throw cleanupAndFail(datasetId, documentId, "索引失败: " + outcome.error());
            }
            sleepBeforeNextPoll(datasetId, documentId);
        }
    }

    /**
     * 删除 Dify 侧异常文档并组装失败异常
     */
    private BusinessException cleanupAndFail(String datasetId, String documentId, String reason) {
        // 拒绝原因只进日志：给用户看的文案里出现"缺少章节编号字段"这类内部规则没有意义，
        // 但运维排查知识库残留时必须能看到
        log.warn("知识库文档入库被拒绝: datasetId={}, documentId={}, reason={}", datasetId, documentId, reason);
        boolean deleted = datasetWebClient.deleteDocument(datasetId, documentId);
        if (!deleted) {
            log.warn("异常文档清理失败，需人工核对知识库残留: datasetId={}, documentId={}", datasetId, documentId);
        }
        return new BusinessException(ErrorMessageConstant.KNOWLEDGE_UPLOAD_REJECTED);
    }

    private void sleepBeforeNextPoll(String datasetId, String documentId) {
        try {
            Thread.sleep(INDEXING_POLL_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // 中断也要清理：网关超时或客户端断开会走到这里，不删就留下活孤儿文档
            throw cleanupAndFail(datasetId, documentId, "索引状态轮询被中断");
        }
    }

    /**
     * 索引状态结论
     *
     * @param status             waiting / parsing / cleaning / splitting / indexing / completed / error
     * @param totalSegments      生成的分段总数，0 表示空壳
     * @param completedSegments  已索引完成的分段数
     * @param error              失败原因，成功时为 null
     */
    private record IndexingOutcome(String status, int totalSegments, int completedSegments, String error) {
    }

    /**
     * 查询并解析索引状态：在 data 数组里按 documentId 精确匹配，
     * 不能直接取 data[0]——一个 batch 含多个文档时会把别人的结论套到本文档上，
     * 导致误判成功或误删正常文档。
     *
     * @return 匹配到的状态；数组为空或状态字段缺失时返回 null 表示继续等待
     */
    private IndexingOutcome fetchIndexingOutcome(String datasetId, String batch, String documentId) {
        String response = datasetWebClient.getIndexingStatus(datasetId, batch);
        try {
            JsonNode dataNode = OBJECT_MAPPER.readTree(response).path("data");
            if (!dataNode.isArray()) {
                return null;
            }
            for (JsonNode node : dataNode) {
                if (!documentId.equals(node.path("id").asText(null))) {
                    continue;
                }
                String status = node.path("indexing_status").asText("");
                if (status.isEmpty()) {
                    return null;
                }
                String error = node.path("error").asText("");
                return new IndexingOutcome(status, node.path("total_segments").asInt(0),
                        node.path("completed_segments").asInt(0), error.isBlank() ? "未知原因" : error);
            }
            return null;
        } catch (Exception e) {
            throw new BusinessException(ErrorMessageConstant.AGENT_RESULT_UNREADABLE, e);
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
            throw new BusinessException(ErrorMessageConstant.AGENT_RESULT_UNREADABLE, e);
        }
    }
}
