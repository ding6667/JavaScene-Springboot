package com.amlve.study.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("study_learning_progress")
public class StudyLearningProgress extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("chapter_id")
    private Long chapterId;

    @TableField("progress_rate")
    private Integer progressRate;

    @TableField("study_duration")
    private Integer studyDuration;

    @TableField("last_study_time")
    private LocalDateTime lastStudyTime;

    @TableField("is_completed")
    private Integer isCompleted;
}