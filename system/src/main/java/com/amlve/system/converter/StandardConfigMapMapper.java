package com.amlve.system.converter;

import com.amlve.common.dto.StandardConfigDTO;
import com.amlve.system.tableEnt.StandardConfig;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StandardConfigMapMapper {
    StandardConfigDTO toDTO(StandardConfig standardConfig);
    StandardConfig toEntity(StandardConfigDTO standardConfigDTO);
}
