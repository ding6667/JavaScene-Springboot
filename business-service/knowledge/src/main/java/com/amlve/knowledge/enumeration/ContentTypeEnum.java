package com.amlve.knowledge.enumeration;

/**
 * 教材原文内容类型
 * <p>
 * 对应 knowledge_content.content_type（tinyint）。任务文档里写的
 * summary/concept/code_example/error_prone/exercise/lab 是"备课七类素材"的口径，
 * 那几类素材分别存在 knowledge_concept / knowledge_exercise / knowledge_code_snippet /
 * knowledge_difficult_point 等表里，与本表的四类型不是一回事，故按表结构定义。
 */
public enum ContentTypeEnum {

    /** 概念定义 */
    CONCEPT(1, "概念定义"),

    /** 例题 */
    EXAMPLE(2, "例题"),

    /** 代码案例 */
    CODE_CASE(3, "代码案例"),

    /** 习题 */
    EXERCISE(4, "习题");

    private final Integer code;

    private final String desc;

    ContentTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 按码值取中文描述
     *
     * @param code 码值，可为 null
     * @return 描述；未匹配返回"未知"
     */
    public static String descOf(Integer code) {
        if (code != null) {
            for (ContentTypeEnum value : values()) {
                if (value.code.equals(code)) {
                    return value.desc;
                }
            }
        }
        return "未知";
    }
}
