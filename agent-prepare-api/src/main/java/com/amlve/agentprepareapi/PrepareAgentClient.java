package com.amlve.agentprepareapi;


public interface PrepareAgentClient {
    /**
     * 调用 Dify 获取教案文本
     */
    String getLessonText(String lessonPrompt);
}
