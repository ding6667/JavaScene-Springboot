package com.amlve.dialogueapi.service;

import com.amlve.dialogueapi.dto.StudentRagInsertDTO;

import java.util.List;

public interface RagService {
    void addContent(List<StudentRagInsertDTO> ragInsertDTOs);
}
