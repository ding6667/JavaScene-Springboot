package com.amlve.dialogue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IntentJsonDTO {
    private Boolean isNewIntent;
    private String intentType;
    private String suggestedType;
    private List<String> suggestedTemplates;
    private String reason;
}
