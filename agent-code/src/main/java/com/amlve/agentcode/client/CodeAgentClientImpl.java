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
 * <p>Mock 兜底：Dify 密钥未配置或调用/解析失败时，返回按错误类型
 * 预置的固定调试结果，保证学生端调试流程始终能跑通。
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
        // 1. 密钥未配置 → 直接 Mock 兜底
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Dify 代码调试密钥未配置（dify.code-api-key），返回 Mock 调试结果");
            return Result.success(buildMockResponse(request));
        }
        // 2. 调用 Dify 工作流；任何异常都兜底为 Mock，不向上抛
        try {
            Map<String, Object> inputs = buildInputs(request);
            String response = difyClient.runWorkflowBlocking(apiKey, inputs);
            return Result.success(parseResponse(response));
        } catch (Exception e) {
            log.warn("Dify 代码调试工作流调用失败，返回 Mock 调试结果: {}", e.getMessage());
            log.debug("Dify 调用失败堆栈", e);
            return Result.success(buildMockResponse(request));
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
        try {
            JsonNode root = OBJECT_MAPPER.readTree(response);
            JsonNode dataNode = root.path("data");
            String status = dataNode.path("status").asText("");
            if (!"succeeded".equals(status)) {
                String error = dataNode.path("error").asText("未知错误");
                throw new IllegalStateException(
                        "Dify 工作流执行失败: status=" + status + ", error=" + error);
            }
            JsonNode outputs = dataNode.path("outputs");
            if (outputs.isMissingNode() || outputs.isNull()) {
                throw new IllegalStateException("Dify 工作流响应缺少 outputs");
            }

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
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("解析 Dify 调试响应失败: " + e.getMessage(), e);
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

    // ============ Mock 兜底 ============

    /**
     * 按错误类型返回固定调试结果。
     * 仅解释错误、引导修改，不输出完整代码；完整丰富版字段给最小可读占位，
     * 保证响应结构与真实调用一致，前端与落库不会因字段缺失而报错。
     */
    private CodeDebugResponse buildMockResponse(CodeDebugRequest request) {
        String errorType = request.getErrorType() == null ? "" : request.getErrorType();
        CodeDebugResponse mock = new CodeDebugResponse();
        if (request.getErrorLine() != null && !request.getErrorLine().isBlank()) {
            mock.setErrorLineList(List.of(request.getErrorLine()));
        } else {
            mock.setErrorLineList(List.of());
        }
        switch (errorType) {
            case "syntax_error" -> {
                mock.setErrorReason("代码未能通过编译，存在语法错误（如缺少分号、括号不匹配、类型不匹配等）。");
                mock.setOptimizeSuggest("请根据编译错误提示定位到首个报错行，重点检查该行及其上一行的拼写、标点和类型使用。");
                mock.setStandardIdea("编写 Java 代码时，建议先保证代码可编译通过，再逐步完善功能逻辑。");
                mock.setKnowledgePoints(List.of("Java 语法基础", "编译错误定位"));
            }
            case "runtime_error" -> {
                mock.setErrorReason("程序在运行期间发生异常（常见于数组越界、空指针、除零、类型转换等）。");
                mock.setOptimizeSuggest("请查看异常堆栈中提示的行号，检查数组下标边界、对象是否为空、除数是否为零等常见问题。");
                mock.setStandardIdea("运行时异常通常源于边界条件未处理好，建议先明确异常类型，再检查对应变量的取值范围。");
                mock.setKnowledgePoints(List.of("常见运行时异常", "边界条件检查"));
            }
            case "logic_error" -> {
                mock.setErrorReason("程序可以运行，但输出与预期不一致，存在逻辑错误。");
                mock.setOptimizeSuggest("请对照题目的测试用例，检查循环边界、条件判断与输出格式是否与要求一致。");
                mock.setStandardIdea("逻辑错误建议用「最小失败用例」定位：只保留一个未通过的用例，人工模拟每一步的中间值。");
                mock.setKnowledgePoints(List.of("测试用例比对", "循环边界", "条件逻辑"));
            }
            case "timeout" -> {
                mock.setErrorReason("程序运行时间超出限制，可能存在死循环或低效算法。");
                mock.setOptimizeSuggest("检查循环终止条件是否可能永远不成立，并评估算法的时间复杂度。");
                mock.setStandardIdea("先确认循环变量的变化方向，再考虑能否用更优的数据结构或提前退出的方式降低复杂度。");
                mock.setKnowledgePoints(List.of("循环终止条件", "复杂度估算"));
            }
            case "memory_overflow" -> {
                mock.setErrorReason("程序内存占用超出限制，可能存在大对象分配或内存泄漏。");
                mock.setOptimizeSuggest("避免在循环内反复创建大对象，检查集合是否会无限增长。");
                mock.setStandardIdea("评估每个大对象的生命周期，尽量复用对象并缩小集合初始容量。");
                mock.setKnowledgePoints(List.of("内存管理", "对象生命周期"));
            }
            default -> {
                mock.setErrorReason("AI 调试服务暂时不可用，暂时无法给出针对本题的详细分析。");
                mock.setOptimizeSuggest("请稍后重试，或先对照教材检查代码逻辑（输入输出、循环边界、变量初始化）。");
                mock.setStandardIdea("先梳理程序的整体思路：输入是什么、输出是什么、中间步骤是否正确。");
                mock.setKnowledgePoints(List.of("程序调试方法"));
            }
        }
        CodeQualityDTO quality = new CodeQualityDTO();
        quality.setOverall(3);
        quality.setReview("AI 调试服务暂时不可用，暂未给出各维度评分。");
        mock.setCodeQuality(quality);
        mock.setEncouragement("调试是学习编程最重要的一环，再试一次！");
        mock.setLearningSuggestion("建议结合教材对应章节复习本题涉及的知识点。");
        return mock;
    }
}
