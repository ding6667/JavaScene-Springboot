package com.amlve.file.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface StorageService {
    String store(MultipartFile file,String fileId) throws IOException;
    Integer delete(String filePath);
}
