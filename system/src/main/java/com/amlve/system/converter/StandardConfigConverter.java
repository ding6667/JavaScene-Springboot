package com.amlve.system.converter;

import com.amlve.system.dto.request.StandardConfigRequest;
import com.amlve.systemapi.dto.api.StandardConfigResponse;
import com.amlve.system.tableEnt.StandardConfig;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StandardConfigConverter {
    StandardConfigResponse toDTO(StandardConfig standardConfig);
    StandardConfig toEntity(StandardConfigResponse standardConfigResponse);
    StandardConfig toEntity(StandardConfigRequest standardConfigRequest);


}