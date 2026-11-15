package com.amlve.agentscoreapi.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 作业批改请求（传给 Dify 的入参）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreEvaluateRequest {

    /**
     * 题目ID
     */
    private String questionId;

    /**
     * 题型：1=选择，2=填空，3=判断，4=程序阅读，5=编程题，6=简答题
     */
    private String questionType;

    /**
     * 题目内容
     */
    private String questionContent;

    /**
     * 标准答案
     */
    private String standardAnswer;

    /**
     * 学生作答
     */
    private String studentAnswer;

    /**
     * 本题满分
     */
    private String fullScore;

    /**
     * 章节ID
     */
    private String chapterId;

    /**
     * 章节名称（供 LLM 参考）
     */
    private String chapterName;

    /**
     * 本题考查的知识点（逗号分隔，供 LLM 参考评分维度）
     */
    private String knowledgePoints;
}
