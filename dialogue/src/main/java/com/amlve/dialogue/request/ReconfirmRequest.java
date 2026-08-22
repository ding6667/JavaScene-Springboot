package com.amlve.dialogue.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * LLM 在流式输出末尾输出的确认请求 JSON 结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReconfirmRequest {
    private String type;           // 固定值 "reconfirm_request"
    private String message;        // 确认框展示内容（Markdown 格式）
    private String toolName;       // Spring Bean 名称
    private Map<String, Object> toolParams; // 工具参数
}