package com.amlve.agentcode.client;

import com.amlve.agentcode.bean.DifyClient;
import com.amlve.agentcodeapi.dto.request.CodeDebugRequest;
import com.amlve.agentcodeapi.dto.response.CodeDebugResponse;
import com.amlve.agentcodeapi.dto.response.CodeQualityDTO;
import com.amlve.agentcodeapi.dto.response.CommonMistakeDTO;
import com.amlve.agentcodeapi.dto.response.ComplexityDTO;
import com.amlve.agentcodeapi.dto.response.ImprovementDTO;
import com.amlve.agentcodeapi.dto.response.RelatedKnowledgeDTO;
import com.amlve.agentcodeapi.dto.response.SimilarQuestionDTO;
import com.amlve.agentcodeapi.dto.response.StepDTO;
import com.amlve.agentcodeapi.service.CodeAgentClient;
import com.amlve.common.constant.ErrorMessageConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代码调试智能体客户端实现
 *
 * <p>职责：把 {@link CodeDebugRequest} 组装为 Dify 工作流 inputs（14 个入参，
 * 与任务书 2.1 字段映射表一一对应：code / chapter_id / chapter_name / question_id /
 * question_content / question_standard_answer / question_requirements / error_type /
 * error_msg / error_line / run_result / test_result / passed_count / total_count），
 * 调用代码调试工作流（blocking），并把 data.outputs 解析为完整丰富版
 * {@link CodeDebugResponse}，供 study 模块使用。
 *
 * <p>失败口径：不再提供 Mock 兜底。Mock 会返回一份看起来合法的"AI 分析"，学生照着它改代码，
 * 而它跟本次提交没有任何关系——错而不报错比失败更糟。密钥缺失、服务不可达、工作流未成功、
 * 响应结构异常一律如实返回失败，技术细节只进日志，消息只给用户能看懂的一句话。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeAgentClientImpl implements CodeAgentClient {

    /**
     * 忽略未知字段：Dify 响应的 data 含 workflow_run_id 等大量额外字段
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** 代码调试工作流应用密钥（app- 开头） */
    @Value("${dify.code-api-key:}")
    private String apiKey;

    private final DifyClient difyClient;

    @Override
    public Result<CodeDebugResponse> debug(CodeDebugRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("代码调试智能体未配置密钥（dify.code-api-key），本次不提供 AI 分析");
            return Result.error(ErrorMessageConstant.AI_DEBUG_UNAVAILABLE);
        }
        // 传输层失败单独兜住：DifyClient 的异常消息带 HTTP 状态和原始响应体，属于排障信息，不能透传给学生
        String response;
        try {
            response = difyClient.runWorkflowBlocking(apiKey, buildInputs(request));
        } catch (Exception e) {
            log.warn("调用代码调试工作流失败: {}", e.getMessage(), e);
            return Result.error(ErrorMessageConstant.AI_DEBUG_UNAVAILABLE);
        }
        try {
            return Result.success(parseResponse(response));
        } catch (BusinessException e) {
            // parseResponse 抛出的已是面向用户的文案，技术细节在抛出点记过日志
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.warn("处理代码调试响应失败: {}", e.getMessage(), e);
            return Result.error(ErrorMessageConstant.AI_DEBUG_UNREADABLE);
        }
    }

    // ============ 请求组装 ============

    /**
     * 组装 Dify 工作流 inputs（14 个入参，任务书 2.1 完整版）。
     * 可选字段为 null 时不传（Dify 开始节点可选变量允许缺失，但传 null 可能校验失败）。
     */
    private Map<String, Object> buildInputs(CodeDebugRequest request) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("code", defaultString(request.getCode()));
        putIfNotBlank(inputs, "chapter_id", request.getChapterId());
        putIfNotBlank(inputs, "chapter_name", request.getChapterName());
        putIfNotBlank(inputs, "question_id", request.getQuestionId());
        putIfNotBlank(inputs, "question_content", request.getQuestionContent());
        putIfNotBlank(inputs, "question_standard_answer", request.getQuestionStandardAnswer());
        putIfNotBlank(inputs, "question_requirements", request.getQuestionRequirements());
        putIfNotBlank(inputs, "error_type", request.getErrorType());
        putIfNotBlank(inputs, "error_msg", request.getErrorMsg());
        putIfNotBlank(inputs, "error_line", request.getErrorLine());
        putIfNotBlank(inputs, "run_result", request.getRunResult());
        putIfNotBlank(inputs, "test_result", request.getTestResult());
        putIfNotBlank(inputs, "passed_count", request.getPassedCount());
        putIfNotBlank(inputs, "total_count", request.getTotalCount());
        return inputs;
    }

    private void putIfNotBlank(Map<String, Object> inputs, String key, String value) {
        if (value != null && !value.isBlank()) {
            inputs.put(key, value);
        }
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    // ============ 响应解析 ============

    /**
     * 解析 Dify blocking 响应（完整丰富版）：data.status 必须为 succeeded，
     * 再从 data.outputs 读取基础错误信息 + codeQuality + complexity + steps +
     * commonMistakes + improvements + relatedKnowledge + similarQuestions +
     * encouragement + learningSuggestion。
     */
    private CodeDebugResponse parseResponse(String response) {
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(response);
        } catch (Exception e) {
            log.warn("代码调试工作流响应不是合法 JSON: {}", e.getMessage());
            throw new BusinessException(ErrorMessageConstant.AI_DEBUG_UNREADABLE);
        }

        JsonNode dataNode = root.path("data");
        String status = dataNode.path("status").asText("");
        if (!"succeeded".equals(status)) {
            log.warn("代码调试工作流未执行成功: status={}, error={}",
                    status, dataNode.path("error").asText(""));
            throw new BusinessException(ErrorMessageConstant.AI_DEBUG_UNAVAILABLE);
        }
        JsonNode outputs = dataNode.path("outputs");
        if (outputs.isMissingNode() || outputs.isNull()) {
            log.warn("代码调试工作流响应缺少 outputs 节点");
            throw new BusinessException(ErrorMessageConstant.AI_DEBUG_UNREADABLE);
        }

        try {
            CodeDebugResponse result = new CodeDebugResponse();
            result.setErrorLineList(readStringList(outputs.path("errorLineList")));
            result.setErrorReason(readText(outputs.path("errorReason")));
            result.setOptimizeSuggest(readText(outputs.path("optimizeSuggest")));
            result.setStandardIdea(readText(outputs.path("standardIdea")));
            result.setKnowledgePoints(readStringList(outputs.path("knowledgePoints")));
            result.setCodeQuality(parseCodeQuality(outputs.path("codeQuality")));
            result.setComplexity(readObject(outputs.path("complexity"), ComplexityDTO.class));
            result.setSteps(readObjectList(outputs.path("steps"), StepDTO.class));
            result.setCommonMistakes(readObjectList(outputs.path("commonMistakes"), CommonMistakeDTO.class));
            result.setImprovements(readObjectList(outputs.path("improvements"), ImprovementDTO.class));
            result.setRelatedKnowledge(readObjectList(outputs.path("relatedKnowledge"), RelatedKnowledgeDTO.class));
            result.setSimilarQuestions(readObjectList(outputs.path("similarQuestions"), SimilarQuestionDTO.class));
            result.setEncouragement(readText(outputs.path("encouragement")));
            result.setLearningSuggestion(readText(outputs.path("learningSuggestion")));
            return result;
        } catch (Exception e) {
            log.warn("解析代码调试响应字段失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorMessageConstant.AI_DEBUG_UNREADABLE);
        }
    }

    /** 字段可能是字符串数组，也可能是整型/其它标量数组，统一转 List&lt;String&gt; */
    private List<String> readStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                if (item != null && !item.isNull()) {
                    list.add(item.asText());
                }
            }
        }
        return list;
    }

    /** 读取单个结构化对象（缺失或为 null 时返回 null） */
    private <T> T readObject(JsonNode node, Class<T> type) {
        if (node == null || node.isNull() || node.isMissingNode() || !node.isObject()) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(node, type);
    }

    /**
     * 解析代码质量评分：维度分值逐键读取；
     * 「各维度点评」在 Dify 出参中可能落在 comment（字符串）或 review 键，
     * comment 为数值时按「注释质量」维度分值处理（任务书原稿两字段重名，见 CodeQualityDTO#review 注释）。
     */
    private CodeQualityDTO parseCodeQuality(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode() || !node.isObject()) {
            return null;
        }
        CodeQualityDTO dto = new CodeQualityDTO();
        dto.setReadability(intOrNull(node, "readability"));
        dto.setNaming(intOrNull(node, "naming"));
        dto.setStructure(intOrNull(node, "structure"));
        dto.setComplexityControl(intOrNull(node, "complexityControl"));
        dto.setReusability(intOrNull(node, "reusability"));
        dto.setOverall(intOrNull(node, "overall"));
        JsonNode commentNode = node.path("comment");
        if (commentNode.isNumber()) {
            dto.setComment(commentNode.asInt());
        } else if (commentNode.isTextual()) {
            dto.setReview(commentNode.asText());
        }
        JsonNode reviewNode = node.path("review");
        if (reviewNode.isTextual()) {
            dto.setReview(reviewNode.asText());
        }
        return dto;
    }

    private Integer intOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asInt() : null;
    }

    /** 读取结构化对象数组（缺失或为 null 时返回 null） */
    private <T> List<T> readObjectList(JsonNode node, Class<T> type) {
        if (node == null || node.isNull() || node.isMissingNode() || !node.isArray()) {
            return null;
        }
        List<T> list = new ArrayList<>();
        for (JsonNode item : node) {
            if (item != null && !item.isNull() && item.isObject()) {
                list.add(OBJECT_MAPPER.convertValue(item, type));
            }
        }
        return list;
    }

    private String readText(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        return node.asText();
    }
}
