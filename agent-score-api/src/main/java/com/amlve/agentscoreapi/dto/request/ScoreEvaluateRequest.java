package com.amlve.agentscoreapi.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;


/**
 * Dify 批改请求 DTO
 */
@Data
public class ScoreEvaluateRequest {

    /**
     * 学生答案
     */
    @NotBlank(message = "学生答案不能为空")
    private String studentAnswer;

    /**
     * 标准答案
     */
    @NotBlank(message = "标准答案不能为空")
    private String standardAnswer;

    /**
     * 题型（1=选择，2=填空，3=判断，4=程序阅读，5=编程题）
     */
    @NotNull(message = "题型不能为空")
    private Integer questionType;

    /**
     * 题目满分
     */
    @NotNull(message = "满分值不能为空")
    private Integer fullScore;

    /**
     * 题目内容（可选，供AI参考）
     */
    private String questionContent;

    /**
     * 章节ID（可选，用于知识库检索）
     */
    private Long chapterId;

    /**
     * 题目ID
     */
    private Long questionId;

    /**
     * 章节名称（可选，供AI参考）
     */
    private String chapterName;

    /**
     * 知识点名称列表（可选，用于精准补救）
     */
    private List<String> knowledgePoints;
}