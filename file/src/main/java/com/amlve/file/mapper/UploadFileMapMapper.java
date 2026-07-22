package com.amlve.file.mapper;

import com.amlve.common.dto.UploadFileDTO;
import com.amlve.file.tableEnt.UploadFile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UploadFileMapMapper {
    UploadFileDTO toDto (UploadFile uploadFile);
    UploadFile toEntity (UploadFileDTO uploadFileDTO);
}
