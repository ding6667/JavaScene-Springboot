package com.amlve.exam.enumeration;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 题目生成状态枚举
 */
public enum GenerateStatusEnum {

    SUCCESS("success", "全部成功"),
    PARTIAL("partial", "部分成功"),
    ERROR("error", "全部失败");

    @JsonValue
    private final String code;
    private final String desc;

    GenerateStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static GenerateStatusEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (GenerateStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
