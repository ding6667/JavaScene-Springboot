package com.amlve.dialogueapi.dto.api;

import com.amlve.dialogueapi.entity.CommentJson;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentRagInsertDTO {

    private String userId;
    private String taskId;
    private String text;
    private Float score;
    private String studentId;
    private CommentJson commentJson;
    private Long submitTime;
    private String semester;
}