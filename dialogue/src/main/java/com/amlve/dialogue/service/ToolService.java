package com.amlve.dialogue.service;

import com.amlve.dialogue.entity.PendingOperation;
import com.amlve.dialogue.enumeration.Intent;
import com.amlve.dialogue.enumeration.TargetScope;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;

public interface ToolService {
    String executeTool(String toolName, Map<String, Object> params);
    String buildExecutePrompt(PendingOperation pending, String toolResult);
    List<ToolCallback> getToolsForIntent(Intent intent, TargetScope bestTargetScope);
}
