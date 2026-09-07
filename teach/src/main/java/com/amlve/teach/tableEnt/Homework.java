package com.amlve.teach.tableEnt;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业表实体，对应数据库 homework 表
 */
@Data
@TableName("homework")
public class Homework {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作业标题 */
    private String title;

    /** 作业内容/要求 */
    private String content;

    /** 关联教材章节ID */
    private Long chapterId;

    /** 作业截止时间 */
    private LocalDateTime deadline;

    /** 发布教师ID */
    private Long teacherId;

    /** 创建时间 */
    private LocalDateTime createTime;
}
