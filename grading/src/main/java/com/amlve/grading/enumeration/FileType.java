package com.amlve.grading.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum FileType {
    WORD("WORD", "Word文档"),
    ZIP("ZIP", "压缩包");

    @EnumValue
    private final String code;
    @JsonValue
    private final String desc;

    FileType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static FileType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (FileType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}