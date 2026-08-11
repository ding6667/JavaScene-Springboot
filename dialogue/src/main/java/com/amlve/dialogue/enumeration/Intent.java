package com.amlve.dialogue.enumeration;

import lombok.Getter;

@Getter
public enum Intent {
    INDIVIDUAL("个体", "individual",false),
    GROUP("群体", "group",false),
    TREND("趋势", "trend",false),   // 需要时间解析能力
    DIAGNOSE("诊断", "diagnosis",false), // 需要多轮推理
    COMPARE("对比", "compare",false), // 需要多轮推理
    SUGGEST("建议", "suggestion",false),    // 需要历史数据挖掘
    ALERT("预警", "alert",false),       // 仅限管理员
    COMMAND("命令", "command",true),
    OTHER("其他", "other",false), //普通对话
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
            if (intent.getCode().equals(code)) {
                return intent;
            }
        }
        return UNKNOWN;
    }
}
