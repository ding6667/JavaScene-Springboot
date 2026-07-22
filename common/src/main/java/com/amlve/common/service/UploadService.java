package com.amlve.common.service;

import com.amlve.common.dto.UploadFileDTO;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    UploadFileDTO selectById(String id);
    Integer updateById(UploadFileDTO uploadFileDTO);
}
