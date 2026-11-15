package com.amlve.teach.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 作业题目快照实体，对应数据库 teach_homework_question 表。
 * 发布作业时把题目内容固化存档，后续查看不再实时依赖 exam 模块。
 * options/answer/knowledge_points 为数据库 JSON 列，以字符串形式存取；
 * render_config 需要结构化读取（前端要取 blankCount / choiceMode），用 JacksonTypeHandler 映射为 JsonNode。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "teach_homework_question", autoResultMap = true)
public class TeachHomeworkQuestion extends BaseEntity {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作业ID */
    @TableField("homework_id")
    private Long homeworkId;

    /** 题目ID（关联 exam_question） */
    @TableField("question_id")
    private Long questionId;

    /** 来源：1教材习题生成 2AI生成 3人工录入 */
    @TableField("source_type")
    private Integer sourceType;

    /** 题干快照（发布时固化） */
    private String content;

    /** 简短题目快照：一句话概括题意（如「最长k个相等字符串」） */
    @TableField("short_title")
    private String shortTitle;

    /**
     * 前端渲染配置快照（JSON 对象）：填空 {@code {"blankCount":3}}、
     * 选择 {@code {"choiceMode":"single"|"multiple"}} 等，发布时从题库一并固化。
     */
    @TableField(value = "render_config", typeHandler = JacksonTypeHandler.class)
    private JsonNode renderConfig;

    /** 题型：1选择 2填空 3判断 4程序阅读 5编程 6简答 */
    private Integer type;

    /** 选项快照（选择题用，JSON 数组字符串） */
    private String options;

    /** 标准答案快照（教师可见，学生端不返回） */
    private String answer;

    /** 解析快照 */
    private String analysis;

    /**
     * 编程题代码框架快照：学生打开作业时用它初始化编辑器，
     * 固化后题库再改框架也不影响已发布作业。
     */
    @TableField("code_framework")
    private String codeFramework;

    /** 作答要求快照（JSON 数组）：主观题/编程题的作答约束，学生端可见 */
    @TableField(value = "requirements", typeHandler = JacksonTypeHandler.class)
    private JsonNode requirements;

    /** 阶梯提示快照（JSON 数组）：学生按需逐条展开 */
    @TableField(value = "hints", typeHandler = JacksonTypeHandler.class)
    private JsonNode hints;

    /** 解题思路快照：做错后才展示，与解析分开控制 */
    @TableField("solution_outline")
    private String solutionOutline;

    /**
     * 测试用例快照（JSON 数组）：错题重做时按这一版用例判题。
     * <b>判题凭据，任何面向学生的 DTO 都不得包含该字段。</b>
     */
    @TableField(value = "test_cases", typeHandler = JacksonTypeHandler.class)
    private JsonNode testCases;

    /** 判题方式快照：1=AI批改 2=测试用例判题 3=混合 */
    @TableField("judge_mode")
    private Integer judgeMode;

    /** 难度：1基础 2进阶 3拔高 */
    /** 难度名称快照（如 简单/进阶/拔高），发布时从题库难度配置固化，不随配置变更 */
    private String difficulty;

    /** 章节ID快照 */
    @TableField("chapter_id")
    private Long chapterId;

    /** 知识点快照（JSON 数组字符串） */
    @TableField("knowledge_points")
    private String knowledgePoints;

    /** 题目默认分值（快照，score 为空时兜底） */
    @TableField("default_score")
    private Integer defaultScore;

    /** 教材页码（教材题快照） */
    private Integer page;

    /** 题目排序 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 本题分值（NULL 则使用题目默认分值） */
    private Integer score;
}
