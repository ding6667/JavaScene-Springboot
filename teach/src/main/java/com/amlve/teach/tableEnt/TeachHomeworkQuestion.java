package com.amlve.teach.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 作业题目快照实体，对应数据库 teach_homework_question 表。
 * 发布作业时把题目内容固化存档，后续查看不再实时依赖 exam 模块。
 * options/answer/knowledge_points 为数据库 JSON 列，以字符串形式存取。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("teach_homework_question")
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

    /** 题型：1选择 2填空 3判断 4程序阅读 5编程 6简答 */
    private Integer type;

    /** 选项快照（选择题用，JSON 数组字符串） */
    private String options;

    /** 标准答案快照（教师可见，学生端不返回） */
    private String answer;

    /** 解析快照 */
    private String analysis;

    /** 难度：1基础 2进阶 3拔高 */
    private Integer difficulty;

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
