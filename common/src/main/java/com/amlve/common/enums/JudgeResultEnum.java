package com.amlve.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 判题结果枚举
 */
public enum JudgeResultEnum {

    ALL_PASSED(1, "全部通过"),
    PARTIAL(0, "部分通过"),
    NOT_JUDGED(-1, "未判题");

    @EnumValue
    private final Integer code;

    @JsonValue
    private final String desc;

    JudgeResultEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }

    public String getDesc() { return desc; }

    @JsonCreator
    public static JudgeResultEnum fromDesc(String desc) {
        if (desc == null) return null;
        for (JudgeResultEnum value : values()) {
            if (value.desc.equals(desc)) return value;
        }
        return null;
    }

    public static JudgeResultEnum fromCode(Integer code) {
        if (code == null) return null;
        for (JudgeResultEnum value : values()) {
            if (value.code.equals(code)) return value;
        }
        return null;
    }
}