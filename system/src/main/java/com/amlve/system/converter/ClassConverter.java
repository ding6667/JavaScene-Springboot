package com.amlve.system.converter;

import com.amlve.system.dto.request.ClassRequest;
import com.amlve.system.dto.response.ClassResponse;
import com.amlve.system.tableEnt.ClassInfo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClassConverter {

    ClassInfo toEntity(ClassRequest request);

    ClassResponse toResponse(ClassInfo classTable);
}