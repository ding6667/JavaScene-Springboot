package com.amlve.systemapi.service;

import com.amlve.systemapi.dto.StandardConfigDTO;

public interface StandardService {
    StandardConfigDTO selectByName(String name);
    StandardConfigDTO selectById(Long id);
}
