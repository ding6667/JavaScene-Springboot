package com.amlve.dialogue.service;

import com.amlve.dialogue.enumeration.Intent;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

public interface RouterLlmService {
    Intent resolveIntent(String query);
    List<ToolCallback> getToolsForIntent(Intent intent);
}
