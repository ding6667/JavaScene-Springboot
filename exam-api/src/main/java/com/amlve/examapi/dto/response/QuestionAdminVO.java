package com.amlve.examapi.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端题目视图（教师/管理员可见，含答案与解析）。
 * <p>
 * 与出题智能体产出字段保持同一套口径，手动录入与 AI 出题在管理端展示一致。
 */
@Data
public class QuestionAdminVO {

    private Long id;

    /**
     * 题干
     */
    private String content;

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
     * 标准答案
     */
    private String answer;

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
    private Integer difficulty;

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