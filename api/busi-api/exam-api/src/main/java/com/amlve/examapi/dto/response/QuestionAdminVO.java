package com.amlve.examapi.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端题目视图（教师/管理员可见，含答案与解析）。
 * <p>
 * 与出题智能体产出字段保持同一套口径，手动录入与 AI 出题在管理端展示一致。
 * <p>
 * answer 直接回传库内结构化对象（{@code {"correct":["A"]}}、{@code {"blanks":["a","b"]}}、
 * {@code {"correct":true}}、{@code {"output":"9"}}、{@code {"reference":"..."}}），
 * 前端原样回填到 {@code QuestionSaveRequest.answer} 即可，不再做字符串互转。
 */
@Data
public class QuestionAdminVO {

    private Long id;

    /**
     * 题干
     */
    private String content;

    /**
     * 简短题目：一句话概括题意（如「最长k个相等字符串」），列表/卡片直接用
     */
    private String shortTitle;

    /**
     * 前端渲染配置（JSON 对象）：选择 {@code {"choiceMode":"single"|"multiple"}}、
     * 填空 {@code {"blankCount":3}}、编程 {@code {"language":"java"}} 等；
     * 题目保存时原样回填即可
     */
    private JsonNode renderConfig;

    /**
     * 题型：1=选择，2=填空，3=判断，4=程序阅读，5=编程，6=简答
     */
    private Integer type;

    /**
     * 题型描述
     */
    private String typeDesc;

    /**
     * 选项（选择题用）
     */
    private List<String> options;

    /**
     * 标准答案（结构化对象，按题型定形）
     */
    private JsonNode answer;

    /**
     * 解析
     */
    private String analysis;

    /**
     * 关联章节ID
     */
    private Long chapterId;

    /**
     * 难度：1=基础，2=进阶，3=拔高
     */
    private Long difficultyConfigId;
    private String difficultyName;

    /**
     * 原始难度分（1.0-5.0）
     */
    private BigDecimal difficultyScore;

    /**
     * 认知层次
     */
    private String cognitiveLevel;

    /**
     * 默认分值
     */
    private Integer score;

    /**
     * 组卷权重
     */
    private BigDecimal weight;

    /**
     * 判题方式：1=AI批改，2=测试用例判题，3=混合
     */
    private Integer judgeMode;

    /**
     * 主观题要求
     */
    private List<String> requirements;

    /**
     * 编程题代码框架
     */
    private String codeFramework;

    /**
     * 阶梯提示
     */
    private List<String> hints;

    /**
     * 解题思路概述
     */
    private String solutionOutline;

    /**
     * 能力维度
     */
    private List<String> abilityDimensions;

    /**
     * 常见错误
     */
    private List<String> commonMistakes;

    /**
     * 知识点ID列表
     */
    private List<Long> knowledgePointIds;

    /**
     * 编程题测试用例（修改页面回填用，学生端不下发）
     */
    private List<QuestionTestCaseVO> testCases;

    /**
     * 相似题ID列表
     */
    private List<Long> relatedQuestionIds;

    /**
     * 总提交次数
     */
    private Integer submitCount;

    /**
     * 通过次数
     */
    private Integer acceptedCount;

    /**
     * 通过率（%）
     */
    private BigDecimal acceptanceRate;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}