package com.amlve.agentscore.client;

import com.amlve.agentscore.bean.DifyClient;
import com.amlve.agentscoreapi.dto.request.ScoreEvaluateRequest;
import com.amlve.agentscoreapi.dto.response.ScoreEvaluateResponse;
import com.amlve.agentscoreapi.service.ScoreClient;
import com.amlve.common.exception.BusinessException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评分智能体客户端实现。
 * 已配置 {@code dify.score-api-key} 时调用真实 Dify 批改工作流并解析响应；
 * 未配置时走 Mock 兜底，便于本地无 Dify 环境跑通链路。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScoreClientImpl implements ScoreClient {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final int MOCK_SCORE = 80;

    private final DifyClient difyClient;

    @Value("${dify.score-api-key:}")
    private String scoreApiKey;

    @Override
    public ScoreEvaluateResponse evaluate(ScoreEvaluateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("评分请求不能为空");
        }
        if (request.getFullScore() == null || request.getFullScore().isBlank()) {
            throw new IllegalArgumentException("题目满分不能为空");
        }
        if (scoreApiKey == null || scoreApiKey.isBlank()) {
            log.warn("未配置 dify.score-api-key，评分智能体走 Mock 兜底");
            return mockResponse(request);
        }
        Map<String, Object> inputs = buildInputs(request);
        String response = difyClient.runWorkflowBlocking(scoreApiKey, inputs);
        return parseResponse(response, request);
    }

    private Map<String, Object> buildInputs(ScoreEvaluateRequest request) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("question_id", nullToEmpty(request.getQuestionId()));
        inputs.put("question_type", nullToEmpty(request.getQuestionType()));
        inputs.put("question_content", nullToEmpty(request.getQuestionContent()));
        inputs.put("standard_answer", nullToEmpty(request.getStandardAnswer()));
        inputs.put("student_answer", nullToEmpty(request.getStudentAnswer()));
        inputs.put("full_score", nullToEmpty(request.getFullScore()));
        inputs.put("chapter_id", nullToEmpty(request.getChapterId()));
        inputs.put("chapter_name", nullToEmpty(request.getChapterName()));
        inputs.put("knowledge_points", nullToEmpty(request.getKnowledgePoints()));
        return inputs;
    }

    private ScoreEvaluateResponse parseResponse(String response, ScoreEvaluateRequest request) {
        try {
            JsonNode root = MAPPER.readTree(response);
            JsonNode data = root.path("data");
            String status = data.path("status").asText("");
            if (!"succeeded".equals(status)) {
                String error = data.path("error").asText("未知错误");
                throw new BusinessException("Dify 批改工作流执行失败: status=" + status + ", error=" + error);
            }
            JsonNode outputs = data.path("outputs");
            JsonNode source = outputs.path("output").isObject() ? outputs.path("output") : outputs;

            ScoreEvaluateResponse result = new ScoreEvaluateResponse();
            result.setScore(source.path("score").isNumber() ? source.path("score").asInt() : null);
            result.setIsCorrect(source.path("is_correct").isNumber() ? source.path("is_correct").asInt() : null);
            result.setAnalysis(source.path("analysis").asText(null));
            result.setSuggestion(source.path("suggestion").asText(null));
            result.setKnowledgePoints(source.path("knowledge_points").asText(null));

            ScoreEvaluateResponse.ErrorDetail errorDetail = new ScoreEvaluateResponse.ErrorDetail();
            errorDetail.setErrors(parseErrors(source));
            result.setErrorDetail(errorDetail);
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("解析 Dify 批改工作流响应失败: " + e.getMessage(), e);
        }
    }

    private List<ScoreEvaluateResponse.ErrorItem> parseErrors(JsonNode source) {
        JsonNode errorsNode = source.path("error_detail").path("errors");
        List<ScoreEvaluateResponse.ErrorItem> items = new ArrayList<>();
        if (errorsNode.isArray()) {
            for (JsonNode n : errorsNode) {
                ScoreEvaluateResponse.ErrorItem item = new ScoreEvaluateResponse.ErrorItem();
                item.setLocation(n.path("location").asText(null));
                item.setErrorType(n.path("errorType").asText(null));
                item.setErrorDescription(n.path("errorDescription").asText(null));
                item.setReason(n.path("reason").asText(null));
                item.setSuggestion(n.path("suggestion").asText(null));
                items.add(item);
            }
        }
        return items;
    }

    private ScoreEvaluateResponse mockResponse(ScoreEvaluateRequest request) {
        ScoreEvaluateResponse response = new ScoreEvaluateResponse();
        response.setScore(Math.min(MOCK_SCORE, parseFullScore(request.getFullScore())));
        response.setIsCorrect(0);
        response.setAnalysis("答案基本正确，当前使用固定批改结果。");
        response.setSuggestion("请结合标准答案复习相关知识点。");

        ScoreEvaluateResponse.ErrorItem item = new ScoreEvaluateResponse.ErrorItem();
        item.setLocation("整体");
        item.setErrorType("待完善");
        item.setErrorDescription("当前为固定批改结果，暂未进行真实答案分析。");
        item.setReason("评分智能体尚未接入真实批改服务。");
        item.setSuggestion("后续接入真实评分模型后获取详细错误原因。");

        ScoreEvaluateResponse.ErrorDetail errorDetail = new ScoreEvaluateResponse.ErrorDetail();
        errorDetail.setErrors(List.of(item));
        response.setErrorDetail(errorDetail);
        return response;
    }

    private int parseFullScore(String fullScore) {
        try {
            return Integer.parseInt(fullScore);
        } catch (Exception e) {
            return MOCK_SCORE;
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
