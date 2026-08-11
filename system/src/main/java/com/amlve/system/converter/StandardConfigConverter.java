package com.amlve.system.converter;

import com.amlve.system.dto.StandardConfigRequest;
import com.amlve.systemapi.dto.StandardConfigDTO;
import com.amlve.system.tableEnt.StandardConfig;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StandardConfigConverter {
    StandardConfigDTO toDTO(StandardConfig standardConfig);
    StandardConfig toEntity(StandardConfigDTO standardConfigDTO);
    StandardConfig toEntity(StandardConfigRequest standardConfigRequest);


}
