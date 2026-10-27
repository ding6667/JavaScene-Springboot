package com.amlve.agentscore.client;

import com.amlve.agentscore.bean.DifyClient;
import com.amlve.agentscoreapi.enumeration.ErrorTypeEnum;
import com.amlve.agentscoreapi.dto.request.ScoreEvaluateRequest;
import com.amlve.agentscoreapi.dto.response.ScoreEvaluateResponse;
import com.amlve.agentscoreapi.service.ScoreAgentClient;
import com.amlve.common.constant.ErrorMessageConstant;
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
 * 评分智能体客户端实现：调用 Dify 批改工作流并解析响应。
 * <p>
 * 不再提供 Mock 兜底。Mock 会返回一个看起来合法的固定分数，批改结果错而不报错，
 * 比直接失败更糟；三个调用方（作业批改、主观题判分、编程题判分）都已捕获异常并
 * 降级为「本次不记分 + 记日志」，因此配置缺失会如实暴露。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScoreClientImpl implements ScoreAgentClient {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final DifyClient difyClient;

    @Value("${dify.score-api-key:}")
    private String scoreApiKey;

    @Override
    public ScoreEvaluateResponse evaluate(ScoreEvaluateRequest request) {
        // 入参由 exam / study 两个调用方现场构造，fullScore 一律来自 String.valueOf(题目分值)，
        // 原先两道判空校验分支不可达，且抛 IllegalArgumentException 会被兜底成
        // 「系统异常，请稍后重试」，消息反而到不了用户，故移除
        if (scoreApiKey == null || scoreApiKey.isBlank()) {
            log.warn("评分智能体未配置密钥（dify.score-api-key），本次无法批改");
            throw new BusinessException(ErrorMessageConstant.AI_GRADING_UNAVAILABLE);
        }
        // 传输层失败单独兜住：DifyClient 的异常消息带 HTTP 状态与原始响应体，属于排障信息，不能透传给学生
        String response;
        try {
            response = difyClient.runWorkflowBlocking(scoreApiKey, buildInputs(request));
        } catch (Exception e) {
            log.warn("调用 Dify 批改工作流失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorMessageConstant.AI_GRADING_UNAVAILABLE);
        }
        return parseResponse(response);
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

    private ScoreEvaluateResponse parseResponse(String response) {
        try {
            JsonNode root = MAPPER.readTree(response);
            JsonNode data = root.path("data");
            String status = data.path("status").asText("");
            if (!"succeeded".equals(status)) {
                log.warn("Dify 批改工作流未执行成功: status={}, error={}",
                        status, data.path("error").asText(""));
                throw new BusinessException(ErrorMessageConstant.AI_GRADING_UNAVAILABLE);
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
            log.warn("解析 Dify 批改响应失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorMessageConstant.AI_GRADING_UNREADABLE);
        }
    }

    private List<ScoreEvaluateResponse.ErrorItem> parseErrors(JsonNode source) {
        JsonNode errorsNode = source.path("error_detail").path("errors");
        List<ScoreEvaluateResponse.ErrorItem> items = new ArrayList<>();
        if (errorsNode.isArray()) {
            for (JsonNode n : errorsNode) {
                ScoreEvaluateResponse.ErrorItem item = new ScoreEvaluateResponse.ErrorItem();
                item.setLocation(n.path("location").asText(null));
                item.setErrorType(ErrorTypeEnum.normalize(n.path("errorType").asText(null)));
                item.setErrorDescription(n.path("errorDescription").asText(null));
                item.setReason(n.path("reason").asText(null));
                item.setSuggestion(n.path("suggestion").asText(null));
                items.add(item);
            }
        }
        return items;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
