package com.amlve.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 上传记录状态枚举
 */
public enum UploadStatusEnum {

    PROCESSING(0, "处理中"),
    SUCCESS(1, "成功"),
    FAILED(2, "失败");

    @EnumValue
    private final Integer code;

    @JsonValue
    private final String desc;

    UploadStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }

    public String getDesc() { return desc; }

    @JsonCreator
    public static UploadStatusEnum fromDesc(String desc) {
        if (desc == null) return null;
        for (UploadStatusEnum value : values()) {
            if (value.desc.equals(desc)) return value;
        }
        return null;
    }

    public static UploadStatusEnum fromCode(Integer code) {
        if (code == null) return null;
        for (UploadStatusEnum value : values()) {
            if (value.code.equals(code)) return value;
        }
        return null;
    }
}