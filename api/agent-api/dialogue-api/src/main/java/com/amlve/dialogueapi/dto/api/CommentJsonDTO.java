package com.amlve.dialogueapi.dto.api;

import lombok.Data;
@Data
public class CommentJsonDTO {
    private String title;
    private String scoringTable;
    private String mistakes;
    private String advantages;
    private String summary;
}