package com.amlve.study.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学生练习记录（表：study_practice_record）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("study_practice_record")
public class StudyPracticeRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 学生ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 题目ID
     */
    @TableField("question_id")
    private Long questionId;

    /**
     * 章节ID
     */
    @TableField("chapter_id")
    private Long chapterId;

    /**
     * 练习模式：1=随机，2=章节专项，3=错题重做
     */
    @TableField("practice_mode")
    private Integer practiceMode;

    /**
     * 学生答案
     */
    @TableField("answer_content")
    private String answerContent;

    /**
     * 耗时（秒）
     */
    @TableField("duration")
    private Integer duration;

    /**
     * 尝试次数
     */
    @TableField("attempt_count")
    private Integer attemptCount;

    /**
     * 是否正确
     */
    @TableField("is_correct")
    private Integer isCorrect;

    /**
     * 得分
     */
    @TableField("score")
    private Integer score;

    /**
     * 最后一次运行记录ID
     */
    @TableField("run_record_id")
    private Long runRecordId;

    /**
     * 0=草稿，1=已提交
     */
    @TableField("status")
    private Integer status;

    /**
     * 提交时间
     */
    @TableField("submit_time")
    private LocalDateTime submitTime;
}
