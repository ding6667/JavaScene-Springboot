package com.amlve.dialogue.utils;

import com.amlve.dialogue.annotation.ToolScope;
import com.amlve.dialogue.enumeration.TargetScope;
import jakarta.validation.constraints.NotNull;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ToolsFilterUtil {
    public List<ToolCallback> filterTools(TargetScope targetScope, @NotNull List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream().filter(tool -> {
            ToolScope toolScopeAnnotation = tool.getClass().getAnnotation(ToolScope.class);
            if (toolScopeAnnotation == null) return true;
            if (targetScope == TargetScope.ALL) return true;
            if (toolScopeAnnotation.value() == null) return true;
            return toolScopeAnnotation.value() == targetScope;
        }).toList();
    }
}
