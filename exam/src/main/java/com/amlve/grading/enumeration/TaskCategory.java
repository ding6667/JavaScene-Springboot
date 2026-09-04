package com.amlve.grading.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum TaskCategory {
    HOMEWORK("HOMEWORK", "平时作业"),
    MIDTERM("MIDTERM", "期中作业"),
    FINAL("FINAL", "期末作业"),
    PROJECT("PROJECT", "项目作业");

    @EnumValue
    private final String code;
    @JsonValue
    private final String desc;

    TaskCategory(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TaskCategory fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (TaskCategory category : values()) {
            if (category.getCode().equals(code)) {
                return category;
            }
        }
        return null;
    }
}