package com.amlve.system.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserConfigKeyEnum {
    EXPORT_PATH(1,"导出路径",""),
    EXPORT_FILE_NAME(2,"导出文件名称",""),
    REMIND_CYCLE(3,"即将过期提醒周期","3"),
    HISTORY_CLEAN_CYCLE(4,"历史文件自动清理周期","7"),
    RETRY_POLICY(5,"重试策略","3"),
    DIALOG_API_API_KEY(6,"对话API密钥",""),
    VECTOR_MODEL_API_KEY(7,"向量模型API密钥",""),
    ENABLE_EMAIL_NOTIFICATION(8,"启用批阅后邮箱通知","0"),
    ENABLE_EMAIL_RESULT(9,"启用批阅后结果发送邮箱","0");

    @EnumValue
    private final Integer configKey;
    @JsonValue
    private final String desc;

    private final String defaultValue;

    UserConfigKeyEnum(Integer configKey, String desc,String defaultValue) {
        this.configKey = configKey;
        this.desc = desc;
        this.defaultValue = defaultValue;
    }

    public static UserConfigKeyEnum fromConfigKey(Integer configKey) {
        for (UserConfigKeyEnum value : values()) {
            if (value.configKey.equals(configKey)) {
                return value;
            }
        }
        return null;
    }
}
