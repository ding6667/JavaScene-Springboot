package com.amlve.agentcodeapi.dto.response;

import lombok.Data;

/**
 * 常见错误对比
 */
@Data
public class CommonMistakeDTO {

    /**
     * 学生可能写错的代码
     */
    private String mistake;

    /**
     * 正确的写法
     */
    private String correct;

    /**
     * 为什么会错
     */
    private String reason;
}
