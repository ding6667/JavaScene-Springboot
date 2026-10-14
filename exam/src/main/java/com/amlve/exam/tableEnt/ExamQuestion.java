package com.amlve.exam.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.amlve.exam.enumeration.QuestionDifficultyEnum;
import com.amlve.common.enums.QuestionTypeEnum;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 题目表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "exam_question", autoResultMap = true)
public class ExamQuestion extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 题干
     */
    private String content;

    /**
     * 题型：1=选择，2=填空，3=判断，4=程序阅读，5=编程，6=简答
     */
    private QuestionTypeEnum type;

    /**
     * 选项（JSON 数组，选择题用）
     */
    private String options;

    /**
     * 主观题要求（JSON 数组）
     */
    @TableField(value = "requirements", typeHandler = JacksonTypeHandler.class)
    private List<String> requirements;

    /**
     * 编程题代码框架
     */
    private String codeFramework;

    /**
     * 阶梯提示（JSON 数组）
     */
    @TableField(value = "hints", typeHandler = JacksonTypeHandler.class)
    private List<String> hints;

    /**
     * 相似题ID列表（JSON 数组）
     */
    @TableField(value = "related_question_ids", typeHandler = JacksonTypeHandler.class)
    private List<Long> relatedQuestionIds;

    /**
     * 标准答案（JSON）
     */
    @TableField(value = "answer", typeHandler = JacksonTypeHandler.class)
    private String answer;

    /**
     * 解析
     */
    private String analysis;

    /**
     * 解题思路概述
     */
    @TableField("solution_outline")
    private String solutionOutline;

    /**
     * 关联章节ID
     */
    @TableField("chapter_id")
    private Long chapterId;

    /**
     * 难度：1=基础，2=进阶，3=拔高
     */
    private QuestionDifficultyEnum difficulty;

    /**
     * 原始难度分（1.0-5.0）
     */
    @TableField("difficulty_score")
    private BigDecimal difficultyScore;

    /**
     * 认知层次
     */
    private String cognitiveLevel;

    /**
     * 能力维度（JSON 数组）
     */
    @TableField(value = "ability_dimensions", typeHandler = JacksonTypeHandler.class)
    private List<String> abilityDimensions;

    /**
     * 常见错误（JSON 数组）
     */
    @TableField(value = "common_mistakes", typeHandler = JacksonTypeHandler.class)
    private List<String> commonMistakes;

    /**
     * 知识点ID列表（JSON 数组）
     */
    @TableField(value = "knowledge_points", typeHandler = JacksonTypeHandler.class)
    private List<Long> knowledgePointIds;

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
    @TableField("judge_mode")
    private Integer judgeMode;

    /**
     * 测试用例（JSON 数组）
     */
    @TableField(value = "test_cases", typeHandler = JacksonTypeHandler.class)
    private List<TestCase> testCases;

    /**
     * 总提交次数
     */
    @TableField("submit_count")
    private Integer submitCount;

    /**
     * 通过次数
     */
    @TableField("accepted_count")
    private Integer acceptedCount;

    /**
     * 通过率（%）
     */
    @TableField("acceptance_rate")
    private BigDecimal acceptanceRate;

    /**
     * 测试用例
     */
    @Data
    public static class TestCase {
        /** 用例序号 */
        private Integer caseIndex;
        /** 输入 */
        private String input;
        /** 期望输出 */
        private String expectedOutput;
        /** 是否示例用例（示例下发给学生，其余隐藏） */
        private Boolean isSample;
        /** 用例类型：SAMPLE/NORMAL/BOUNDARY/SPECIAL/PERFORMANCE/ERROR */
        private String caseType;
        /** 分值权重 */
        private Integer weight;
        /** 时间上限（ms） */
        private Integer timeLimit;
        /** 内存上限（KB） */
        private Integer memoryLimit;
        /** 用例说明 */
        private String description;
    }
}
