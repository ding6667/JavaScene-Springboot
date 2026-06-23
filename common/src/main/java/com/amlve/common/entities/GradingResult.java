package com.amlve.common.entities;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 批阅结果明细表实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("grading_result")
public class GradingResult {

    /**
     * 自增主键（数据库自动生成）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的批阅任务ID
     */
    private String taskId;

    /**
     * 生成的 Word 报告存储路径（可为空）
     */
    private String wordPath;

    /**
     * 生成的 Excel 汇总表存储路径（可为空）
     */
    private String excelPath;

    /**
     * 批阅状态：0-已完成 1-失败
     */
    private Integer status;

    /**
     * 记录创建时间
     */
    private LocalDateTime createTime;

    /**
     * 过期时间（用于定时清理）
     */
    private LocalDateTime expireTime;
}