package com.amlve.common.Enum;

import lombok.Getter;

@Getter
public enum GenerateWordType {
    // task 生成word
    TASK("task"),
    // student 生成word
    STUDENT("student");;
    private final String value;
    GenerateWordType(String value) {
        this.value = value;
    }
}
