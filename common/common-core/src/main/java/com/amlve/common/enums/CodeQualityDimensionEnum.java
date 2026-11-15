package com.amlve.common.enums;

/**
 * 代码质量维度枚举
 */
public enum CodeQualityDimensionEnum {

    READABILITY("可读性"),
    NAMING("命名规范"),
    STRUCTURE("代码结构"),
    COMMENT("注释质量"),
    COMPLEXITY("复杂度控制"),
    REUSABILITY("可复用性");

    private final String desc;

    CodeQualityDimensionEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() { return desc; }
}