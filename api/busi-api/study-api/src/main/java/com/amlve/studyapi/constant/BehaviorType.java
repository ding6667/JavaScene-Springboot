package com.amlve.studyapi.constant;

/**
 * 学习行为类型枚举（落库存 code 字符串）
 *
 * <p>为下月 agent-analysis 学情分析积累数据，埋点覆盖：
 * 运行代码、调试代码、进度保存、刷题提交、草稿保存。
 */
public enum BehaviorType {

    /** 进度保存 */
    PROGRESS_SAVE("progress_save", "进度保存"),

    /** 代码运行（/api/study/code/run） */
    CODE_RUN("code_run", "代码运行"),

    /** 代码调试（/api/study/code/debug） */
    CODE_DEBUG("code_debug", "代码调试"),

    /** 刷题正式提交（/api/study/practice/submit） */
    PRACTICE_SUBMIT("practice_submit", "刷题提交"),

    /** 刷题草稿保存（/api/study/practice/draft） */
    PRACTICE_DRAFT("practice_draft", "草稿保存");

    private final String code;
    private final String desc;

    BehaviorType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }

    public static BehaviorType fromCode(String code) {
        if (code == null) return null;
        for (BehaviorType value : values()) {
            if (value.code.equals(code)) return value;
        }
        return null;
    }
}
