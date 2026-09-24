package com.amlve.exam.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 题目难度枚举
 */
public enum QuestionDifficultyEnum {

    BASIC(1, "基础"),
    INTERMEDIATE(2, "进阶"),
    ADVANCED(3, "拔高");
    @EnumValue
    private final Integer code;
    @JsonValue
    private final String desc;

    QuestionDifficultyEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static QuestionDifficultyEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (QuestionDifficultyEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}