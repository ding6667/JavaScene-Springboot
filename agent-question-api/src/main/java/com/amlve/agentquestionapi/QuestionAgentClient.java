package com.amlve.agentquestionapi;

public interface QuestionAgentClient {
    /**
     * 问题
     * @param query
     * @return
     */
    String ask(String query);
}
