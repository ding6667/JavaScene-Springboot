package com.amlve.dialogue.service;

import com.amlve.dialogueapi.dto.api.StudentRagInsertDTO;


import java.util.List;

public interface RagService {
    List<String> queryContent(String query, List<String> taskIds, String userId, Double maxScore, Double minScore);


}