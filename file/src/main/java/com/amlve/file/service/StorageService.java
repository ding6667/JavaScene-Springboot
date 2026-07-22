package com.amlve.file.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {
    String store(MultipartFile file,String fileId) throws IOException;
    Integer delete(String filePath);
}
