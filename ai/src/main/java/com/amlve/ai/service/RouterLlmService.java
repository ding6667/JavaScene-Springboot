package com.amlve.ai.service;

import com.amlve.ai.enumeration.Intent;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

public interface RouterLlmService {
    Intent resolveIntent(String query);
    List<ToolCallback> getToolsForIntent(Intent intent);
}
