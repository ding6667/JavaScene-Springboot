package com.amlve.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 代码运行错误类型枚举（agent-code 用）
 */
public enum CodeErrorTypeEnum {

    SYNTAX_ERROR(1, "syntax_error", "语法错误"),
    RUNTIME_ERROR(2, "runtime_error", "运行时错误"),
    LOGIC_ERROR(3, "logic_error", "逻辑错误"),
    TIMEOUT(4, "timeout", "超时"),
    MEMORY_OVERFLOW(5, "memory_overflow", "内存溢出"),
    SECURITY_VIOLATION(6, "security_violation", "安全违规"),
    NONE(7, "none", "无错误");

    @EnumValue
    private final Integer code;

    /**
     * 传给 Dify 的字符串值
     */
    private final String difyValue;

    @JsonValue
    private final String desc;

    CodeErrorTypeEnum(Integer code, String difyValue, String desc) {
        this.code = code;
        this.difyValue = difyValue;
        this.desc = desc;
    }

    public Integer getCode() { return code; }

    public String getDifyValue() { return difyValue; }

    public String getDesc() { return desc; }

    @JsonCreator
    public static CodeErrorTypeEnum fromDesc(String desc) {
        if (desc == null) return null;
        for (CodeErrorTypeEnum value : values()) {
            if (value.desc.equals(desc)) return value;
        }
        return null;
    }

    public static CodeErrorTypeEnum fromCode(Integer code) {
        if (code == null) return null;
        for (CodeErrorTypeEnum value : values()) {
            if (value.code.equals(code)) return value;
        }
        return null;
    }

    public static CodeErrorTypeEnum fromDifyValue(String difyValue) {
        if (difyValue == null) return null;
        for (CodeErrorTypeEnum value : values()) {
            if (value.difyValue.equals(difyValue)) return value;
        }
        return null;
    }
}