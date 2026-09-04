package com.amlve.grading.dto.request;

import com.amlve.grading.entity.StudentHomework;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContentRequest {
    private List<StudentHomework> files;
}