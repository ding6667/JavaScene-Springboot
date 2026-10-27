package com.amlve.teach.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 备课记录状态枚举，对应 teach_prepare_record.status 列
 */
@Getter
@AllArgsConstructor
public enum PrepareRecordStatusEnum {

    /** 失败（后台生成异常，原因见 error_msg） */
    FAILED(0, "失败"),
    /** 成功（五类材料已回写） */
    SUCCESS(1, "成功"),
    /** 生成中（记录已建，智能体调用与 PPT 转换仍在后台跑） */
    GENERATING(2, "生成中");

    private final Integer code;
    private final String desc;

    /**
     * 根据 code 获取枚举，未匹配时返回 null
     */
    public static PrepareRecordStatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (PrepareRecordStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 根据 code 获取状态名，未匹配时返回空串（避免前端展示 null）
     */
    public static String nameOf(Integer code) {
        PrepareRecordStatusEnum status = of(code);
        return status == null ? "" : status.desc;
    }
}
