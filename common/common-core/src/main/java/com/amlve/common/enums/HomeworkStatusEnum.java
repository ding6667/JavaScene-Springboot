package com.amlve.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 作业状态枚举
 */
public enum HomeworkStatusEnum {

    DRAFT(1, "草稿"),
    PUBLISHED(2, "已发布"),
    CLOSED(3, "已关闭"),
    ARCHIVED(4, "已归档");

    @EnumValue
    private final Integer code;

    @JsonValue
    private final String desc;

    HomeworkStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }

    public String getDesc() { return desc; }

    @JsonCreator
    public static HomeworkStatusEnum fromDesc(String desc) {
        if (desc == null) return null;
        for (HomeworkStatusEnum value : values()) {
            if (value.desc.equals(desc)) return value;
        }
        return null;
    }

    public static HomeworkStatusEnum fromCode(Integer code) {
        if (code == null) return null;
        for (HomeworkStatusEnum value : values()) {
            if (value.code.equals(code)) return value;
        }
        return null;
    }
}