package com.amlve.agentscoreapi.enumeration;


import lombok.Getter;

/**
 * 错误类型枚举
 */
@Getter
public enum ErrorTypeEnum {

    SYNTAX("语法错误", "代码语法不符合Java规范"),
    LOGIC("逻辑错误", "代码逻辑有误，结果不正确"),
    CONCEPT("概念混淆", "对Java概念理解有误"),
    FORMAT("格式错误", "代码格式不规范"),
    MISSING("遗漏", "缺失必要代码或步骤"),
    OTHER("其他", "其他类型错误");

    private final String name;
    private final String description;

    ErrorTypeEnum(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}