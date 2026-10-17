package com.amlve.agentscoreapi.enumeration;

/**
 * 批改错误类型枚举
 */
public enum ErrorTypeEnum {

    SYNTAX_ERROR("语法错误"),
    LOGIC_ERROR("逻辑错误"),
    CONCEPT_ERROR("概念错误"),
    FORMAT_ERROR("格式错误"),
    MISSING("遗漏"),
    OTHER("其他");

    private final String desc;

    ErrorTypeEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 判断给定的 errorType 是否合法
     */
    public static boolean isValid(String type) {
        if (type == null) {
            return false;
        }
        for (ErrorTypeEnum value : values()) {
            if (value.name().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
