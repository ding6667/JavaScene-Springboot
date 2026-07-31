package com.amlve.system.dto;


import com.amlve.common.entity.RubricItem;
import com.baomidou.mybatisplus.annotation.*;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardConfigDTO {


    /**
     * 标准名称
     */
    @NotBlank(message = "标准名称不能为空")
    private String standardName;

    /**
     * 评分标准内容(Markdown)
     */
    @NotBlank(message = "评分标准内容不能为空")
    private List<RubricItem> standardContent;

    /**
     * 是否当前使用: 0=否, 1=是
     */
    @NotBlank(message = "是否当前使用不能为空")
    private Integer isCurrent;

    /**
     * 创建人/来源
     */
    @NotBlank(message = "创建人/来源不能为空")
    private String userId;

}