package com.amlve.common.Entities.TableEnt;


import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评分标准配置表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("standard_config")
public class StandardConfig {

    /**
     * 自增主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 标准名称
     */
    private String standardName;

    /**
     * 评分标准内容(Markdown)
     */
    private String standardContent;

    /**
     * 是否当前使用: 0=否, 1=是
     */
    private Integer isCurrent;

    /**
     * 创建人/来源
     */
    private String createdBy;

    /**
     * 创建时间（由数据库自动生成）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间（由数据库自动生成）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}