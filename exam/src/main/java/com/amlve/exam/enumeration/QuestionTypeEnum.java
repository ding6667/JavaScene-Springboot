package com.amlve.exam.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 题目题型枚举
 */
public enum QuestionTypeEnum {

    CHOICE(1, "选择题"),
    FILL_IN(2, "填空题"),
    JUDGE(3, "判断题"),
    PROGRAM_READ(4, "程序阅读题"),
    PROGRAMMING(5, "编程题");
    @EnumValue
    private final Integer code;
    @JsonValue
    private final String desc;

    QuestionTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static QuestionTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (QuestionTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}