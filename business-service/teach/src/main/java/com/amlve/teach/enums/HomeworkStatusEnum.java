package com.amlve.teach.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 作业状态枚举，对应 teach_homework.status 列
 */
@Getter
@AllArgsConstructor
public enum HomeworkStatusEnum {

    /** 未发布（草稿） */
    DRAFT(1, "未发布"),
    /** 已发布 */
    PUBLISHED(2, "已发布"),
    /** 已截止（教师关闭或到期） */
    CLOSED(3, "已截止"),
    /** 已归档 */
    ARCHIVED(4, "已归档");

    private final Integer code;
    private final String desc;

    /**
     * 根据 code 获取枚举，未匹配时返回 null
     */
    public static HomeworkStatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (HomeworkStatusEnum status : values()) {
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
        HomeworkStatusEnum status = of(code);
        return status == null ? "" : status.desc;
    }
}
