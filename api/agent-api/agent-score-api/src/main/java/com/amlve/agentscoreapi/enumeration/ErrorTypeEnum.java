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

    /**
     * 归一化 Dify 返回的 errorType。
     * <p>
     * LLM 可能吐出拼写变体或中文描述，原样入库会让下游按枚举统计/筛选全部失配，
     * 因此不认识或为空时统一落到 OTHER，而不是保留脏值
     */
    public static String normalize(String difyValue) {
        if (difyValue == null || difyValue.isBlank()) {
            return OTHER.name();
        }
        String upper = difyValue.trim().toUpperCase();
        for (ErrorTypeEnum value : values()) {
            if (value.name().equals(upper) || value.desc.equals(difyValue.trim())) {
                return value.name();
            }
        }
        return OTHER.name();
    }
}
