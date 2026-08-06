package com.amlve.system.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserConfigKeyEnum {
    STANDARD_CONFIG("standardConfig","评分标准配置");

    @EnumValue
    private final String configKey;
    @JsonValue
    private final String desc;
    UserConfigKeyEnum(String configKey, String desc) {
        this.configKey = configKey;
        this.desc = desc;
    }
}
