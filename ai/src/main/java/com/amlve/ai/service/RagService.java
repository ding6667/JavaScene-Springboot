package com.amlve.ai.service;

import com.amlve.ai.dto.StudentRagInsertDTO;

import com.amlve.ai.enumeration.Intent;
import org.springframework.ai.tool.ToolCallback;


import java.util.List;

public interface RagService {
    List<String> queryContent(String query, List<String> taskIds, String userId, Double maxScore, Double minScore);
    void addContent(List<StudentRagInsertDTO> ragInsertDTOs);

}
