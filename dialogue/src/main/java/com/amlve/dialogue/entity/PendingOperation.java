package com.amlve.dialogue.entity;

import com.amlve.dialogue.enumeration.Intent;
import com.amlve.dialogue.enumeration.TargetScope;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.tool.ToolCallback;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingOperation implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String operationId;
    private String originalMessage;
    private Intent intent;
    private TargetScope targetScope;
    private String targetName;
    private String toolName;              // 待调用的工具 Bean 名称
    private Map<String, Object> toolParams; // 工具参数
    private String confirmMessage;        // 确认框展示内容
    private LocalDateTime createTime;
    private LocalDateTime expireTime;

    public boolean isExpired() {
        return expireTime != null && LocalDateTime.now().isAfter(expireTime);
    }
}