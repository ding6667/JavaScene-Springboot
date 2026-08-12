package com.amlve.grading.dto;

import com.amlve.common.entity.RubricItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RubricRequest {
    private List<RubricItem> rubric;
}
