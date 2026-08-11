package com.amlve.systemapi.dto;

import com.amlve.common.entity.RubricItem;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class StandardConfigDTO {
    private Long id;
    private String standardName;
    private List<RubricItem> standardContent;
    private Integer isCurrent;
    private String createdBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
