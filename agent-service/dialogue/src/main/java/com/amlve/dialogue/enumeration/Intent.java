package com.amlve.dialogue.enumeration;

import lombok.Getter;

@Getter
public enum Intent {
    QUERY("查询", "query",false),
    ANALYZE("分析", "analyze",false),
    EXECUTE("执行", "execute",true),
    CHAT("聊天", "chat",false),

    NEW_INTENT("新意图", "new_intent",false),
    UNKNOWN("未知意图", "unknown",false);



    private final String name;
    private final String code;
    private final Boolean isWriteOperation;

    Intent(String name, String code,Boolean isWriteOperation) {
        this.name = name;
        this.code = code;
        this.isWriteOperation = isWriteOperation;
    }

    public static Intent fromCode(String code) {
        for (Intent intent : values()) {
            if (intent.getCode().equalsIgnoreCase(code)) {
                return intent;
            }
        }
        return UNKNOWN;
    }
}
