package com.amlve.file.converter;

import com.amlve.fileapi.dto.UploadFileDTO;
import com.amlve.file.tableEnt.UploadFile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UploadFileMapMapper {
    UploadFileDTO toDto (UploadFile uploadFile);
    UploadFile toEntity (UploadFileDTO uploadFileDTO);
}
