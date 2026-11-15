package com.amlve.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 批改任务状态枚举
 */
public enum GradingStatusEnum {

    PENDING(0, "待批改"),
    PROCESSING(1, "批改中"),
    DONE(2, "完成"),
    FAILED(3, "失败");

    @EnumValue
    private final Integer code;

    @JsonValue
    private final String desc;

    GradingStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }

    public String getDesc() { return desc; }

    @JsonCreator
    public static GradingStatusEnum fromDesc(String desc) {
        if (desc == null) return null;
        for (GradingStatusEnum value : values()) {
            if (value.desc.equals(desc)) return value;
        }
        return null;
    }

    public static GradingStatusEnum fromCode(Integer code) {
        if (code == null) return null;
        for (GradingStatusEnum value : values()) {
            if (value.code.equals(code)) return value;
        }
        return null;
    }
}