package com.amlve.dialogue.service.tools;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueryTools {

    @Tool(name = "queryIndividualRawScore" ,description = "查询姓名为${studentName}的个人原始分数")
    public String queryIndividualRawScore(String studentName) {
        return "38";
    }
}
