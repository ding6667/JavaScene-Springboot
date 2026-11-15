package com.amlve.dialogue.entity.metadata;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentResultMetadata {
    private String userId;
    private String taskId;
    private String score;
    private String studentId;
    private String aspectType;
    private String submitTime;
    private Long parentId;
    private String semester;

}
