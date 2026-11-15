package com.amlve.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 代码运行状态枚举（力扣式）
 */
public enum RunStatusEnum {

    ACCEPTED("AC", "通过"),
    WRONG_ANSWER("WA", "答案错误"),
    TIME_LIMIT_EXCEEDED("TLE", "超时"),
    MEMORY_LIMIT_EXCEEDED("MLE", "内存超限"),
    RUNTIME_ERROR("RE", "运行时错误"),
    COMPILE_ERROR("CE", "编译错误"),
    OUTPUT_LIMIT_EXCEEDED("OLE", "输出超限"),
    SECURITY_VIOLATION("SV", "安全拦截"),
    SYSTEM_ERROR("SE", "系统错误");

    @EnumValue
    private final String code;

    @JsonValue
    private final String desc;

    RunStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }

    public String getDesc() { return desc; }

    @JsonCreator
    public static RunStatusEnum fromDesc(String desc) {
        if (desc == null) return null;
        for (RunStatusEnum value : values()) {
            if (value.desc.equals(desc)) return value;
        }
        return null;
    }

    public static RunStatusEnum fromCode(String code) {
        if (code == null) return null;
        for (RunStatusEnum value : values()) {
            if (value.code.equals(code)) return value;
        }
        return null;
    }
}