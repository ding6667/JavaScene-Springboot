package com.amlve.teach.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 题型枚举，对应 teach_homework_question.type 列（与 exam 模块题型定义一致）
 */
@Getter
@AllArgsConstructor
public enum QuestionTypeEnum {

    /** 选择题 */
    CHOICE(1, "选择题"),
    /** 填空题 */
    FILL(2, "填空题"),
    /** 判断题 */
    JUDGE(3, "判断题"),
    /** 程序阅读题 */
    CODE_READING(4, "程序阅读题"),
    /** 编程题 */
    CODING(5, "编程题"),
    /** 简答题 */
    SHORT_ANSWER(6, "简答题");

    private final Integer code;
    private final String desc;

    /**
     * 根据 code 获取题型名，未匹配时返回空串
     */
    public static String nameOf(Integer code) {
        if (code == null) {
            return "";
        }
        for (QuestionTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type.desc;
            }
        }
        return "";
    }
}
