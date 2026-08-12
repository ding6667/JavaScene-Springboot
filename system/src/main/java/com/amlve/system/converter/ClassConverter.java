package com.amlve.system.converter;

import com.amlve.system.dto.ClassRequest;
import com.amlve.system.tableEnt.ClassInfo;
import com.amlve.system.vo.ClassResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClassConverter {

    ClassInfo toEntity(ClassRequest request);

    ClassResponse toResponse(ClassInfo classTable);
}