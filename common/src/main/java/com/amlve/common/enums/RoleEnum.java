package com.amlve.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 角色枚举
 */
public enum RoleEnum {

    STUDENT(1, "STUDENT", "学生"),
    TEACHER(2, "TEACHER", "教师"),
    ADMIN(3, "ADMIN", "管理员");

    @EnumValue
    private final Integer code;

    /**
     * 角色名（用于 Spring Security 匹配）
     */
    private final String roleName;

    @JsonValue
    private final String desc;

    RoleEnum(Integer code, String roleName, String desc) {
        this.code = code;
        this.roleName = roleName;
        this.desc = desc;
    }

    public Integer getCode() { return code; }

    public String getRoleName() { return roleName; }

    public String getDesc() { return desc; }

    @JsonCreator
    public static RoleEnum fromDesc(String desc) {
        if (desc == null) return null;
        for (RoleEnum value : values()) {
            if (value.desc.equals(desc)) return value;
        }
        return null;
    }

    public static RoleEnum fromCode(Integer code) {
        if (code == null) return null;
        for (RoleEnum value : values()) {
            if (value.code.equals(code)) return value;
        }
        return null;
    }

    public static RoleEnum fromRoleName(String roleName) {
        if (roleName == null) return null;
        for (RoleEnum value : values()) {
            if (value.roleName.equals(roleName)) return value;
        }
        return null;
    }
}