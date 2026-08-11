package com.amlve.fileapi.service;

import com.amlve.fileapi.dto.UploadFileDTO;


public interface UploadService {
    UploadFileDTO selectById(String id);
    Integer updateById(UploadFileDTO uploadFileDTO);
}
