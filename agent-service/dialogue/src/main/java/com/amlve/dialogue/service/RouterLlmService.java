package com.amlve.dialogue.service;

import com.amlve.dialogue.enumeration.Intent;
import com.amlve.dialogue.enumeration.TargetScope;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;

public interface RouterLlmService {
    Map<String, Object> resolveIntent(String query,String apiKey);

}
