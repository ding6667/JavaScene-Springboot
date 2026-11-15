package com.amlve.dialogue.entity.metadata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IntentMetadata {
    private String intentType;
    private String targetScope;
}
