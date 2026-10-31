package com.amlve.exam.dto.request;

import com.amlve.exam.tableEnt.ExamQuestion;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 题目新增/更新请求。
 * <p>
 * 字段与出题智能体产出对齐，保证手动录入题目在学生端刷题与判题链路同样可用。
 */
@Data
public class QuestionSaveRequest {

    /**
     * 题目ID（更新时必填）
     */
    private Long id;

    @NotBlank(message = "题目内容不能为空")
    private String content;

    /**
     * 简短题目：一句话概括题意（如「最长k个相等字符串」），用于列表/卡片展示，可不传。
     */
    @Size(max = 100, message = "简短题目不能超过100个字符")
    private String shortTitle;

    /**
     * 前端渲染配置（JSON 对象），按题型定形，可不传（后端按题型补齐）：
     * <p>
     * 选择 {@code {"choiceMode":"single"}} / {@code {"choiceMode":"multiple"}}；
     * 填空 {@code {"blankCount":3}}；编程 {@code {"language":"java"}}。
     * <p>
     * 未传时：选择题按答案个数推断单选/多选，填空题按题干 {@code ____} 数量推断空数。
     * 未识别的额外键原样入库，前端可自行扩展渲染参数。
     */
    private JsonNode renderConfig;

    @NotNull(message = "题型不能为空")
    private Integer type;

    private List<String> options;

    /**
     * 标准答案（结构化对象，后端按题型原样落 JSON 列）。
     * <p>
     * 各题型约定形状：
     * 选择 {@code {"correct":["A","B"]}}、填空 {@code {"blanks":["a","b"]}}、
     * 判断 {@code {"correct":true}}、程序阅读 {@code {"output":"9"}}、
     * 编程/简答 {@code {"reference":"..."}}。
     * <p>
     * 为兼容老前端，也接受纯文本（如 {@code "A"}），后端会按题型转成上面的结构。
     */
    @NotNull(message = "标准答案不能为空")
    private JsonNode answer;

    private String analysis;

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
     * 知识点ID列表
     */
    private List<Long> knowledgePointIds;

    /**
     * 解题思路概述
     */
    private String solutionOutline;

    /**
     * 组卷权重
     */
    private BigDecimal weight;

    /**
     * 判题方式：1=AI批改，2=测试用例判题，3=混合
     */
    private Integer judgeMode;

    /**
     * 测试用例
     */
    private List<ExamQuestion.TestCase> testCases;

    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    /** 难度配置ID（exam_question_difficulty_config.id），取自 /api/exam/difficulty-config/list */
    private Long difficultyConfigId;

    /**
     * 原始难度分（1.0-5.0）
     */
    private BigDecimal difficultyScore;

    private Integer score;

    private String cognitiveLevel;

    /**
     * 能力维度
     */
    private List<String> abilityDimensions;

    /**
     * 常见错误
     */
    private List<String> commonMistakes;

    /**
     * 相似题ID列表
     */
    private List<Long> relatedQuestionIds;
}
