package com.amlve.common.service;

import com.amlve.common.dto.StandardConfigDTO;

public interface StandardService {
    StandardConfigDTO selectByName(String name);
    StandardConfigDTO selectById(String id);
    Integer update(StandardConfigDTO standardConfigDTO);
}
