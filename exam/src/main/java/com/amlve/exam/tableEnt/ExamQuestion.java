package com.amlve.exam.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.amlve.exam.enumeration.QuestionDifficultyEnum;
import com.amlve.exam.enumeration.QuestionTypeEnum;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 题目表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_question")
public class ExamQuestion extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String content;

    private QuestionTypeEnum type;

    private String options;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private QuestionAnswer answer;

    @TableField("chapter_id")
    private Long chapterId;

    private QuestionDifficultyEnum difficulty;

    private Integer score;

    /**
     * 标准答案与解析
     */
    @Data
    public static class QuestionAnswer {
        /** 标准答案 */
        private String answer;
        /** 答案解析 */
        private String anlysis;
    }
}