package com.amlve.common.Entities;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class WorkFlowRequest {
    private Map<String, Object> inputs;      // 变量名 -> 值（支持字符串或 List<FileInput>）
    private String responseMode;             // "streaming" 或 "blocking"
    private String user;                     // 用户标识，必填
    private List<Map<String, Object>> files; // 可选，顶层文件列表（若 inputs 中已包含可忽略）
    private String traceId;                  // 可选，链路追踪ID
}
