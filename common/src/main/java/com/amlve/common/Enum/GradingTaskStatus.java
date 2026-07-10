package com.amlve.common.Enum;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum GradingTaskStatus {
    //任务状态：0-待处理 1-处理中 2-已完成 3-已过期 4-失败(可重试) 5-已取消
    PENDING(0,"待处理"),
    PROCESSING(1,"处理中"),
    SUCCESS(2,"已完成"),
    EXPIRED(3,"已过期"),
    FAILED(4,"失败"),
    CANCELLED(5,"已取消");


    @EnumValue
    private final Integer value;
    @JsonValue
    private final String desc;

    GradingTaskStatus(Integer value,String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static GradingTaskStatus getValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (GradingTaskStatus status : values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        return null;
    }
}

