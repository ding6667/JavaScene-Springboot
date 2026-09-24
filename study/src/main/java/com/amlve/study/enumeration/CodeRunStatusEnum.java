package com.amlve.study.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 代码运行状态枚举
 *
 * <p>状态码与 study_code_run_record.status 列（Integer）对应，
 * 实体字段仍为 Integer，业务代码中通过 {@link #getCode()} 取值。
 */
public enum CodeRunStatusEnum {

    /** 运行成功 */
    SUCCESS(1, "运行成功"),
    /** 编译错误 */
    COMPILE_ERROR(2, "编译错误"),
    /** 运行时错误（含超时强制终止） */
    RUNTIME_ERROR(3, "运行时错误"),
    /** 未通过安全检查（命中黑名单 API） */
    SECURITY_REJECTED(4, "未通过安全检查"),
    /** 系统内部错误 */
    INTERNAL_ERROR(5, "系统内部错误");

    @EnumValue
    private final Integer code;
    @JsonValue
    private final String desc;

    CodeRunStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }
    public String getDesc() { return desc; }

    public static CodeRunStatusEnum fromCode(Integer code) {
        if (code == null) return null;
        for (CodeRunStatusEnum value : values()) {
            if (value.code.equals(code)) return value;
        }
        return null;
    }
}
