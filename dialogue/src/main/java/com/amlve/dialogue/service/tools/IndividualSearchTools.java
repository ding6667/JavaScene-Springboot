package com.amlve.dialogue.service.tools;

import com.amlve.dialogue.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;

@Component
@RequiredArgsConstructor
public class IndividualSearchTools {

    private final RagService ragService;

    @Tool(description = "根据用户问题关键词检索相关评语，评分数据")
    public List<String> queryContent(@ToolParam(description = "用户问题中提炼出的关键词,多个关键词用逗号分隔，必填") String query,
                             @ToolParam(description = "taskId,一个或多个taskId,为空或null时查询全部") List<String> taskIds,
                             @ToolParam(description = "当前用户的userId,必填,一个")String userId,
                             @ToolParam(description = "最大评分,默认100")Double maxScore,
                             @ToolParam(description = "最小评分,默认0")Double minScore){
        List<String> strings = ragService.queryContent(query, taskIds, userId, maxScore, minScore);
        if (strings == null || strings.isEmpty()) {
            return null;
        }
        return strings;
    }
}
