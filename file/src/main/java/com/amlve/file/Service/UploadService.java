package com.amlve.file.Service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UploadService {

    String uploadZip(MultipartFile file) throws IOException;
}
