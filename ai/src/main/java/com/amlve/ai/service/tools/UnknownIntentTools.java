package com.amlve.ai.service.tools;

import org.springframework.ai.tool.annotation.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UnknownIntentTools {
    @Tool(description = "当用户问题无法被识别时，返回默认回答")
    public String answer() {
        return "抱歉，目前我无法理解您的问题。您可以尝试换种方式描述您的问题。";
    }
}
