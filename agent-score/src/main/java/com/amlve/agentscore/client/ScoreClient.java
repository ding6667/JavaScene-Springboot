package com.amlve.agentscore.client;

import com.amlve.agentscoreapi.ScoreAgentClient;
import com.amlve.agentscoreapi.dto.request.ScoreEvaluateRequest;
import com.amlve.agentscoreapi.dto.response.ScoreEvaluateResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 评分智能体客户端。
 * 当前阶段使用固定结果，后续可在此处替换为真实的 Dify 调用。
 */
@Component
public class ScoreClient implements ScoreAgentClient {

    private static final int MOCK_SCORE = 80;

    @Override
    public ScoreEvaluateResponse evaluate(ScoreEvaluateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("评分请求不能为空");
        }
        if (request.getFullScore() == null || request.getFullScore() <= 0) {
            throw new IllegalArgumentException("题目满分必须大于 0");
        }

        ScoreEvaluateResponse response = new ScoreEvaluateResponse();
        response.setScore(Math.min(MOCK_SCORE, request.getFullScore()));
        response.setAnalysis("答案基本正确，当前使用固定批改结果。");
        response.setSuggestion("请结合标准答案复习相关知识点。");

        ScoreEvaluateResponse.ErrorDetail detail = new ScoreEvaluateResponse.ErrorDetail();
        detail.setLocation("整体");
        detail.setErrorType("待完善");
        detail.setErrorDescription("当前为固定批改结果，暂未进行真实答案分析。");
        detail.setReason("评分智能体尚未接入真实批改服务。");
        detail.setSuggestion("后续接入真实评分模型后获取详细错误原因。");
        response.setErrorDetails(Collections.singletonList(detail));
        return response;
    }
}
