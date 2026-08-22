package com.amlve.fileapi.service;

import com.amlve.fileapi.dto.UploadFileDTO;

import java.util.Map;


public interface UploadService {
    UploadFileDTO selectById(String id);
    Integer updateById(UploadFileDTO uploadFileDTO);

    Long getFileOwnerId(String fileId);
    Map<String, Object> loadAsResource(String fileId);

}
