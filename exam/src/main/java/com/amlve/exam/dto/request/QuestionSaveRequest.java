package com.amlve.exam.dto.request;

import com.amlve.exam.tableEnt.ExamQuestion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "题型不能为空")
    private Integer type;

    private List<String> options;

    @NotBlank(message = "标准答案不能为空")
    private String answer;

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

    private Integer difficulty;

    private Integer score;

    private String cognitiveLevel;
}
