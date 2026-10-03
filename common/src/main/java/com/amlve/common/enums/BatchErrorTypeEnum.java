package com.amlve.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 批改错误类型枚举（agent-score 用）
 */
public enum BatchErrorTypeEnum {

    SYNTAX_ERROR(1, "语法错误"),
    LOGIC_ERROR(2, "逻辑错误"),
    CONCEPT_ERROR(3, "概念错误"),
    FORMAT_ERROR(4, "格式错误"),
    MISSING(5, "遗漏"),
    OTHER(6, "其他");

    @EnumValue
    private final Integer code;

    @JsonValue
    private final String desc;

    BatchErrorTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }

    public String getDesc() { return desc; }

    @JsonCreator
    public static BatchErrorTypeEnum fromDesc(String desc) {
        if (desc == null) return null;
        for (BatchErrorTypeEnum value : values()) {
            if (value.desc.equals(desc)) return value;
        }
        return null;
    }

    public static BatchErrorTypeEnum fromCode(Integer code) {
        if (code == null) return null;
        for (BatchErrorTypeEnum value : values()) {
            if (value.code.equals(code)) return value;
        }
        return null;
    }

    /**
     * 从 Dify 返回的枚举名字符串还原（如 "SYNTAX_ERROR"）
     */
    public static BatchErrorTypeEnum fromDifyValue(String difyValue) {
        if (difyValue == null) return null;
        for (BatchErrorTypeEnum value : values()) {
            if (value.name().equals(difyValue)) return value;
        }
        return OTHER;
    }

    /**
     * 判断给定的字符串是否合法
     */
    public static boolean isValid(String type) {
        if (type == null) return false;
        for (BatchErrorTypeEnum value : values()) {
            if (value.name().equals(type)) return true;
        }
        return false;
    }
}