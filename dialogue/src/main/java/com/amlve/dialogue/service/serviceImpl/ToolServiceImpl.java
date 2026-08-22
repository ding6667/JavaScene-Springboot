package com.amlve.dialogue.service.serviceImpl;

import com.amlve.dialogue.entity.PendingOperation;
import com.amlve.dialogue.enumeration.Intent;
import com.amlve.dialogue.enumeration.TargetScope;
import com.amlve.dialogue.service.ToolService;
import com.amlve.dialogue.service.tools.AnalyzeTools;
import com.amlve.dialogue.service.tools.ExecuteTools;
import com.amlve.dialogue.service.tools.QueryTools;
import com.amlve.dialogue.service.tools.UnknownIntentTools;
import com.amlve.dialogue.utils.ToolsFilterUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToolServiceImpl implements ToolService {

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    private final ToolsFilterUtil toolsFilterUtil;

    private final UnknownIntentTools unknownIntentTools;
    private final QueryTools queryTools;
    private final AnalyzeTools analyzeTools;
    private final ExecuteTools executeTools;
    /**
     * 执行工具调用
     */
    @Override
    public String executeTool(String toolName, Map<String, Object> params) {
        try {
            // 从 Spring 容器中获取工具 Bean
            ToolCallback tool = applicationContext.getBean(toolName, ToolCallback.class);
            // 将参数转为 JSON 字符串（Spring AI 工具调用要求）
            String paramsJson = objectMapper.writeValueAsString(params);
            // 执行工具
            String result = tool.call(paramsJson);
            log.info("工具执行成功: toolName={}, params={}, result={}", toolName, params, result);
            return result;
        } catch (Exception e) {
            log.error("工具执行失败: toolName={}, error={}", toolName, e.getMessage(), e);
            return "执行失败：" + e.getMessage();
        }
    }

    /**
     * 根据意图返回对应的工具回调列表
     * @param intent 识别出的意图
     * @return 工具回调列表（可能为空）
     */
    public List<ToolCallback> getToolsForIntent(Intent intent, TargetScope bestTargetScope) {
        if (intent == null || intent == Intent.UNKNOWN || intent == Intent.NEW_INTENT) {
            return List.of(ToolCallbacks.from(unknownIntentTools));
        }

        List<ToolCallback> toolCallbacks = switch (intent) {
            case QUERY -> List.of(ToolCallbacks.from(queryTools));
            case ANALYZE -> List.of(ToolCallbacks.from(analyzeTools));
            case EXECUTE -> List.of(ToolCallbacks.from(executeTools));
            default -> List.of();
        };
        return toolsFilterUtil.filterTools(bestTargetScope, toolCallbacks);
    }
    /**
     * 构造执行回执的 Prompt，让 LLM 格式化输出
     */
    public String buildExecutePrompt(PendingOperation pending, String toolResult) {
        return String.format(
                "用户之前请求执行以下操作：%s\n" +
                        "工具已执行完成，返回结果如下：\n%s\n" +
                        "请按以下格式生成最终回复（使用 Markdown）：\n" +
                        "# ✅ 执行完成\n" +
                        "{操作结果摘要}\n" +
                        "## 操作详情\n" +
                        "- 操作前: 已查询\n" +
                        "- 操作后: 已更新\n" +
                        "- 操作时间: %s\n" +
                        "## ⚠️ 回滚提示\n" +
                        "如需撤销，请输入：rollback",
                pending.getOriginalMessage(),
                toolResult,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }
}