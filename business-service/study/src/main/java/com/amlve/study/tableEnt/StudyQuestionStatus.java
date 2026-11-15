package com.amlve.study.tableEnt;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生题目状态（表：study_question_status，任务书 5.4）
 *
 * <p>口径：刷题列表的 done / correct 只从本表取（避免一题多轮运行导致列表抖动），
 * 由刷题提交链路在同一事务内更新；作答内容与草稿状态在 study_practice_record。
 *
 * <p>注意：本表 DDL 无 create_time / update_time / deleted 列，不继承 BaseEntity。
 */
@Data
@TableName("study_question_status")
public class StudyQuestionStatus {

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
     * 0=未做，1=尝试过，2=已通过
     */
    @TableField("status")
    private Integer status;

    /**
     * 尝试次数
     */
    @TableField("attempt_count")
    private Integer attemptCount;

    /**
     * 首次通过时间
     */
    @TableField("first_ac_time")
    private LocalDateTime firstAcTime;

    /**
     * 最后提交时间
     */
    @TableField("last_submit_time")
    private LocalDateTime lastSubmitTime;
}
