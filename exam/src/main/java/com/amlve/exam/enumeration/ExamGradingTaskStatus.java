package com.amlve.exam.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 作业批改任务状态枚举
 */
@Getter
public enum ExamGradingTaskStatus {

    PENDING(0, "待批改"),
    DONE(1, "已完成");

    @EnumValue
    private final Integer value;
    @JsonValue
    private final String desc;

    ExamGradingTaskStatus(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static ExamGradingTaskStatus getValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (ExamGradingTaskStatus status : values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        return null;
    }
}