package com.amlve.grading.dto;

import lombok.Data;

import java.util.List;
@Data
public class GradingStartCommonDTO {
    private List<String> uploadIds;
    private Long standardConfigId;

}

