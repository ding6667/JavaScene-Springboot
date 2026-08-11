package com.amlve.system.vo;


import com.amlve.common.entity.RubricItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardConfigResponse {


    private Long id;

    /**
     * 标准名称
     */
    private String standardName;

    /**
     * 评分标准内容(Markdown)
     */

    private List<RubricItem> standardContent;

    /**
     * 是否当前使用: 0=否, 1=是
     */
    private Integer isCurrent;

    /**
     * 创建人/来源
     */
    private Long userId;
    /**
     * 创建时间（由数据库自动生成）
     */
    private LocalDateTime createTime;

    /**
     * 更新时间（由数据库自动生成）
     */
    private LocalDateTime updateTime;
}