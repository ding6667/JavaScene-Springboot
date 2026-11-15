package com.amlve.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 测试用例类型枚举
 */
public enum TestCaseTypeEnum {

    SAMPLE("SAMPLE", "示例用例"),
    NORMAL("NORMAL", "常规用例"),
    BOUNDARY("BOUNDARY", "边界用例"),
    SPECIAL("SPECIAL", "特殊用例"),
    PERFORMANCE("PERFORMANCE", "性能用例"),
    ERROR("ERROR", "异常用例");

    @EnumValue
    private final String code;

    @JsonValue
    private final String desc;

    TestCaseTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }

    public String getDesc() { return desc; }

    @JsonCreator
    public static TestCaseTypeEnum fromDesc(String desc) {
        if (desc == null) return null;
        for (TestCaseTypeEnum value : values()) {
            if (value.desc.equals(desc)) return value;
        }
        return null;
    }

    public static TestCaseTypeEnum fromCode(String code) {
        if (code == null) return null;
        for (TestCaseTypeEnum value : values()) {
            if (value.code.equals(code)) return value;
        }
        return null;
    }
}