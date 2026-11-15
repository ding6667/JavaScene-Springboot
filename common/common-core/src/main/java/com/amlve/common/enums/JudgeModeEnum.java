package com.amlve.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 判题方式枚举
 */
public enum JudgeModeEnum {

    AI_GRADING(1, "AI批改"),
    TEST_CASE(2, "测试用例判题"),
    MIXED(3, "混合");

    @EnumValue
    private final Integer code;

    @JsonValue
    private final String desc;

    JudgeModeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }

    public String getDesc() { return desc; }

    @JsonCreator
    public static JudgeModeEnum fromDesc(String desc) {
        if (desc == null) return null;
        for (JudgeModeEnum value : values()) {
            if (value.desc.equals(desc)) return value;
        }
        return null;
    }

    public static JudgeModeEnum fromCode(Integer code) {
        if (code == null) return null;
        for (JudgeModeEnum value : values()) {
            if (value.code.equals(code)) return value;
        }
        return null;
    }
}