package com.amlve.file.converter;

import com.amlve.fileapi.dto.api.UploadFileDTO;
import com.amlve.file.tableEnt.UploadFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UploadFileMapMapper {

    @Mapping(target = "fileType", expression = "java(uploadFile.getFileType() != null ? uploadFile.getFileType().getCode() : null)")
    UploadFileDTO toDto (UploadFile uploadFile);
    @Mapping(target = "fileType", ignore = true)
    UploadFile toEntity (UploadFileDTO uploadFileDTO);
}