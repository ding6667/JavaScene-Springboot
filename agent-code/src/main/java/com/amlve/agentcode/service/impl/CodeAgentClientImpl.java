package com.amlve.agentcode.service.impl;

import com.amlve.agentcode.bean.DifyClient;
import com.amlve.agentcodeapi.dto.request.CodeDebugRequest;
import com.amlve.agentcodeapi.dto.response.CodeDebugResponse;
import com.amlve.agentcodeapi.service.CodeAgentClient;
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
 * <p>职责：把 {@link CodeDebugRequest} 组装为 Dify 工作流 inputs，
 * 调用代码调试工作流（blocking），并把 data.outputs 解析为
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
    public CodeDebugResponse debug(CodeDebugRequest request) {
        // 1. 密钥未配置 → 直接 Mock 兜底
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Dify 代码调试密钥未配置（dify.code-api-key），返回 Mock 调试结果");
            return buildMockResponse(request);
        }
        // 2. 调用 Dify 工作流；任何异常都兜底为 Mock，不向上抛
        try {
            Map<String, Object> inputs = buildInputs(request);
            String response = difyClient.runWorkflowBlocking(apiKey, inputs);
            return parseResponse(response);
        } catch (Exception e) {
            log.warn("Dify 代码调试工作流调用失败，返回 Mock 调试结果: {}", e.getMessage());
            log.debug("Dify 调用失败堆栈", e);
            return buildMockResponse(request);
        }
    }

    // ============ 请求组装 ============

    /**
     * 组装 Dify 工作流 inputs。
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
        putIfNotBlank(inputs, "error_type", request.getErrorType());
        putIfNotBlank(inputs, "error_msg", request.getErrorMsg());
        putIfNotBlank(inputs, "error_line", request.getErrorLine());
        putIfNotBlank(inputs, "run_result", request.getRunResult());
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
     * 解析 Dify blocking 响应：data.status 必须为 succeeded，
     * 再从 data.outputs 取 errorLineList/errorReason/optimizeSuggest/
     * standardIdea/knowledgePoints。
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

    private String readText(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        return node.asText();
    }

    // ============ Mock 兜底 ============

    /**
     * 按错误类型返回固定调试结果。
     * 仅解释错误、引导修改，不输出完整代码。
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
            default -> {
                mock.setErrorReason("AI 调试服务暂时不可用，暂时无法给出针对本题的详细分析。");
                mock.setOptimizeSuggest("请稍后重试，或先对照教材检查代码逻辑（输入输出、循环边界、变量初始化）。");
                mock.setStandardIdea("先梳理程序的整体思路：输入是什么、输出是什么、中间步骤是否正确。");
                mock.setKnowledgePoints(List.of("程序调试方法"));
            }
        }
        return mock;
    }
}
