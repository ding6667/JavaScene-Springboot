package com.amlve.agentcodeapi.dto.request;

import lombok.Builder;
import lombok.Data;

/**
 * 传给 Dify 的代码调试请求
 */
@Data
@Builder
public class CodeDebugRequest {

    /**
     * 学生提交的代码
     */
    private String code;

    /**
     * 章节ID
     */
    private String chapterId;

    /**
     * 章节名称（供 LLM 参考）
     */
    private String chapterName;

    /**
     * 题目ID（自由练习时为 null）
     */
    private String questionId;

    /**
     * 题目内容（自由练习时为 null）
     */
    private String questionContent;

    /**
     * 题目标准答案（自由练习时为 null）
     */
    private String questionStandardAnswer;

    /**
     * 错误类型：syntax_error / runtime_error / logic_error / none
     */
    private String errorType;

    /**
     * 编译器/运行时返回的原始错误信息
     */
    private String errorMsg;

    /**
     * 错误行号
     */
    private String errorLine;

    /**
     * 成功运行时的输出内容
     */
    private String runResult;
}