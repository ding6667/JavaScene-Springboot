package com.amlve.system.mapper;

import com.amlve.common.dto.StandardConfigDTO;
import com.amlve.system.tableEnt.StandardConfig;

public interface StandardConfigMapMapper {
    StandardConfigDTO toDTO(StandardConfig standardConfig);
    StandardConfig toEntity(StandardConfigDTO standardConfigDTO);
}
