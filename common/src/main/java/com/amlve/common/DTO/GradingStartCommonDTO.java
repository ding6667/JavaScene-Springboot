package com.amlve.common.DTO;

import lombok.Data;

import java.util.List;
@Data
public class GradingStartCommonDTO {
    private List<String> fileUrls;
    private String rubric;

}

