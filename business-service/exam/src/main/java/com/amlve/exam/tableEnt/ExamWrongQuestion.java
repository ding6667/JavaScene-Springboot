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
 * <p>
 * 只记录「哪个学生、错了哪一版题、来自作业还是刷题、错了几次」，
 * 题面/答案/用例都在 {@link ExamQuestionSnapshot} 里，一张快照被所有学生复用。
 * 唯一键 (user_id, snapshot_id, source)：
 * 同一题的不同版本、不同来源各自一条，不再互相覆盖。
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
     * 题目快照ID（exam_question_snapshot.id）
     */
    private Long snapshotId;

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
