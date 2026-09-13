package com.amlve.agentquestion.client;

import com.amlve.agentquestionapi.service.QuestionAgentClient;
import com.amlve.agentquestionapi.dto.request.QuestionGenerateRequest;
import com.amlve.agentquestionapi.dto.response.QuestionGenerateResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 出题智能体客户端。
 * 当前阶段使用固定示例结果，后续可在此处替换为真实的出题模型调用。
 */
@Component
public class QuestionClient implements QuestionAgentClient {

    @Override
    public QuestionGenerateResponse generate(QuestionGenerateRequest request) {
        QuestionGenerateResponse.QuestionItem item = QuestionGenerateResponse.QuestionItem.builder()
                .id("mock-1")
                .type("correct")
                .difficulty("basic")
                .content("示例题目：请简述 Java 中接口与抽象类的区别（当前为固定出题结果）。")
                .answer("接口允许多继承，抽象类单继承；接口方法默认 public abstract，抽象类可含具体方法。")
                .analysis("当前为固定出题结果，暂未接入真实出题模型。")
                .build();
        return QuestionGenerateResponse.builder()
                .questions(List.of(item))
                .total(1)
                .build();
    }
}
