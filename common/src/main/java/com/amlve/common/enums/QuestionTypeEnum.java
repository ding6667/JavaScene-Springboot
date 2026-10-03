package com.amlve.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 题型枚举
 * 1=选择，2=填空，3=判断，4=程序阅读，5=编程，6=简答
 */
public enum QuestionTypeEnum {

    CHOICE(1, "选择题"),
    FILL_IN(2, "填空题"),
    JUDGE(3, "判断题"),
    PROGRAM_READ(4, "程序阅读题"),
    PROGRAMMING(5, "编程题"),
    SHORT_ANSWER(6, "简答题");

    @EnumValue
    private final Integer code;

    @JsonValue
    private final String desc;

    QuestionTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }

    public String getDesc() { return desc; }

    @JsonCreator
    public static QuestionTypeEnum fromDesc(String desc) {
        if (desc == null) return null;
        for (QuestionTypeEnum value : values()) {
            if (value.desc.equals(desc)) return value;
        }
        return null;
    }

    public static QuestionTypeEnum fromCode(Integer code) {
        if (code == null) return null;
        for (QuestionTypeEnum value : values()) {
            if (value.code.equals(code)) return value;
        }
        return null;
    }

    /**
     * 客观题：可以直接字符串比对判分
     */
    public boolean isObjective() {
        return this == CHOICE || this == FILL_IN || this == JUDGE || this == PROGRAM_READ;
    }

    /**
     * 主观题：需要 AI 批改或跑测试用例
     */
    public boolean isSubjective() {
        return this == PROGRAMMING || this == SHORT_ANSWER;
    }
}