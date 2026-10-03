package com.amlve.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 上传类型枚举
 */
public enum UploadTypeEnum {

    CREATE(1, "新增"),
    UPDATE(2, "更新");

    @EnumValue
    private final Integer code;

    @JsonValue
    private final String desc;

    UploadTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }

    public String getDesc() { return desc; }

    @JsonCreator
    public static UploadTypeEnum fromDesc(String desc) {
        if (desc == null) return null;
        for (UploadTypeEnum value : values()) {
            if (value.desc.equals(desc)) return value;
        }
        return null;
    }

    public static UploadTypeEnum fromCode(Integer code) {
        if (code == null) return null;
        for (UploadTypeEnum value : values()) {
            if (value.code.equals(code)) return value;
        }
        return null;
    }
}