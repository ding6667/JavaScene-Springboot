package com.amlve.grading.dto;

import com.amlve.grading.entity.StudentHomework;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContentDTO {
    private List<StudentHomework> files;
}
