package com.amlve.exam.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 错题归档表实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_wrong_question")
public class ExamWrongQuestion extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 学生ID
     */
    private Long userId;

    /**
     * 题目ID
     */
    private Long questionId;

    /**
     * 来源：1=作业，2=刷题
     */
    private Integer source;

    /**
     * 累计错误次数
     */
    private Integer wrongCount;

    /**
     * 最近一次错误答案
     */
    private String lastAnswer;

    /**
     * 是否已掌握：0=未掌握，1=已掌握
     */
    private Integer mastered;

    /**
     * 最近答错时间
     */
    private LocalDateTime lastWrongTime;
}
