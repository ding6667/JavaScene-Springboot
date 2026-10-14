package com.amlve.agentquestion.client;

import com.amlve.agentquestion.bean.DifyClient;
import com.amlve.agentquestionapi.dto.request.QuestionGenerateRequest;
import com.amlve.agentquestionapi.dto.response.QuestionGenerateResponse;
import com.amlve.agentquestionapi.service.QuestionAgent;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 出题智能体客户端实现。
 * 把 exam 侧组装好的出题请求转为 Dify 工作流入参（单个 input 对象），blocking 调用出题工作流并解析返回的题目列表。
 * 未配置工作流密钥时走 Mock 兜底，便于本地无 Dify 环境跑通链路。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QuestionAgentImpl implements QuestionAgent {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final DifyClient difyClient;

    @Value("${dify.question-api-key}")
    private String questionApiKey;


    @Override
    public Result<QuestionGenerateResponse> generate(QuestionGenerateRequest request) {
        if (questionApiKey == null || questionApiKey.isBlank()) {
            log.warn("未配置 dify.question-api-key，出题智能体走 Mock 兜底返回示例题");
            return Result.success(mockGenerate(request));
        }
        try {
            Map<String, Object> inputs = buildInputs(request);
            String response = difyClient.runWorkflowBlocking(questionApiKey, inputs);
            return Result.success(parseResponse(response));
        } catch (Exception e) {
            log.error("出题智能体调用失败: {}", e.getMessage(), e);
            return Result.success(failedResponse(e.getMessage()));
        }
    }

    /**
     * 出题失败时的兜底响应（status=failed + errorMsg）
     */
    private QuestionGenerateResponse failedResponse(String errorMsg) {
        return QuestionGenerateResponse.builder()
                .questions(List.of())
                .total(0)
                .status("failed")
                .errorMsg(errorMsg)
                .build();
    }

    /**
     * 组装 Dify 工作流入参：start 节点只有一个 json_object 变量 input，
     * 七个子字段均以字符串形式传入（与出题工作流参数提取节点约定一致）
     */
    private Map<String, Object> buildInputs(QuestionGenerateRequest request) {
        Map<String, Object> input = new HashMap<>();
        input.put("chapter_id", String.valueOf(request.getChapterId()));
        input.put("chapter_name", nullToEmpty(request.getChapterName()));
        input.put("allocation", toJson(request.getAllocation()));
        input.put("difficulty", toJson(request.getDifficulty()));
        input.put("question_type", nullToEmpty(request.getQuestionType()));
        input.put("count", String.valueOf(request.getCount()));
        input.put("user_instruction", nullToEmpty(request.getUserInstruction()));

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("input", input);
        return inputs;
    }

    /**
     * 解析 blocking 响应：data.outputs.output 为结束节点结构化输出 { questions, total, status }
     */
    private QuestionGenerateResponse parseResponse(String response) {
        try {
            JsonNode root = MAPPER.readTree(response);
            JsonNode data = root.path("data");
            String status = data.path("status").asText("");
            if (!"succeeded".equals(status)) {
                String error = data.path("error").asText("未知错误");
                throw new BusinessException("Dify 出题工作流执行失败: status=" + status + ", error=" + error);
            }
            JsonNode outputs = data.path("outputs");
            if (outputs.isMissingNode() || outputs.isNull()) {
                throw new BusinessException("Dify 出题工作流响应缺少 outputs: " + response);
            }
            JsonNode outputNode = outputs.path("output");
            JsonNode source = outputNode.isObject() ? outputNode : outputs;

            List<QuestionGenerateResponse.QuestionItem> items = new ArrayList<>();
            JsonNode questionsNode = source.path("questions");
            if (questionsNode.isArray()) {
                for (JsonNode q : questionsNode) {
                    items.add(QuestionGenerateResponse.QuestionItem.builder()
                            .type(q.path("type").asText(null))
                            .difficulty(q.path("difficulty").asText(null))
                            .knowledgePoint(q.path("knowledgePoint").asText(null))
                            .knowledgePoints(toStrList(q.path("knowledgePoints")))
                            .content(q.path("content").asText(null))
                            .options(toStrList(q.path("options")))
                            .answer(q.path("answer").asText(null))
                            .analysis(q.path("analysis").asText(null))
                            .difficultyScore(decimalOrNull(q.path("difficultyScore")))
                            .cognitiveLevel(q.path("cognitiveLevel").asText(null))
                            .requirements(toStrList(q.path("requirements")))
                            .codeFramework(q.path("codeFramework").asText(null))
                            .hints(toStrList(q.path("hints")))
                            .testCases(toTestCaseList(q.path("testCases")))
                            .relatedQuestions(toStrList(q.path("relatedQuestions")))
                            .abilityDimensions(toStrList(q.path("abilityDimensions")))
                            .commonMistakes(toStrList(q.path("commonMistakes")))
                            .solutionOutline(q.path("solutionOutline").asText(null))
                            .weight(decimalOrNull(q.path("weight")))
                            .build());
                }
            }
            return QuestionGenerateResponse.builder()
                    .questions(items)
                    .total(parseInt(source.path("total")))
                    .status(source.path("status").asText(null))
                    .errorMsg(source.path("errorMsg").asText(null))
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("解析 Dify 出题工作流响应失败: " + e.getMessage(), e);
        }
    }

    private QuestionGenerateResponse mockGenerate(QuestionGenerateRequest request) {
        QuestionGenerateResponse.QuestionItem item = QuestionGenerateResponse.QuestionItem.builder()
                .type("choice")
                .difficulty("basic")
                .knowledgePoint("接口与抽象类")
                .knowledgePoints(List.of("接口与抽象类"))
                .difficultyScore(new BigDecimal("1.5"))
                .cognitiveLevel("理解")
                .content("示例题目：请简述 Java 中接口与抽象类的区别（当前为固定出题结果）。")
                .options(List.of("A. 接口可多继承，抽象类单继承", "B. 抽象类可多继承", "C. 接口可含具体方法", "D. 两者完全相同"))
                .answer("A")
                .analysis("当前为固定出题结果，暂未配置 Dify 出题密钥。")
                .build();
        return QuestionGenerateResponse.builder()
                .questions(List.of(item))
                .total(1)
                .status("success")
                .build();
    }

    private String toJson(Object obj) {
        try {
            return obj == null ? "" : MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new BusinessException("序列化出题参数失败: " + e.getMessage(), e);
        }
    }

    private Integer parseInt(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return 0;
        }
        if (node.isIntegralNumber()) {
            return node.asInt();
        }
        try {
            return Integer.parseInt(node.asText());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private BigDecimal decimalOrNull(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        try {
            return new BigDecimal(node.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<String> toStrList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode n : node) {
                result.add(n.isTextual() ? n.asText() : n.toString());
            }
        }
        return result;
    }

    private List<QuestionGenerateResponse.TestCase> toTestCaseList(JsonNode node) {
        List<QuestionGenerateResponse.TestCase> result = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return result;
        }
        for (JsonNode n : node) {
            result.add(QuestionGenerateResponse.TestCase.builder()
                    .caseIndex(n.path("caseIndex").isNumber() ? n.path("caseIndex").asInt() : null)
                    .input(n.path("input").asText(null))
                    .expectedOutput(n.path("expectedOutput").asText(null))
                    .isSample(n.hasNonNull("isSample") ? n.path("isSample").asBoolean() : null)
                    .caseType(n.path("caseType").asText(null))
                    .weight(n.path("weight").isNumber() ? n.path("weight").asInt() : null)
                    .timeLimit(n.path("timeLimit").isNumber() ? n.path("timeLimit").asInt() : null)
                    .memoryLimit(n.path("memoryLimit").isNumber() ? n.path("memoryLimit").asInt() : null)
                    .description(n.path("description").asText(null))
                    .build());
        }
        return result;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
