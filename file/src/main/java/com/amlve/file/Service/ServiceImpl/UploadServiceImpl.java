package com.amlve.file.Service.ServiceImpl;

import com.amlve.common.entities.UploadFile;
import com.amlve.file.Mapper.UploadMapper;
import com.amlve.file.Service.StorageService;
import com.amlve.file.Service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private final UploadMapper uploadMapper;
    private final StorageService storageService;

    @Value("${temp.zip}")
    private String tempZip;


    @Override
    public String uploadZip(MultipartFile file) throws IOException {
        // 生成随机uploadId 组装实体
        String uploadId = UUID.randomUUID().toString().replace("-", "");

        String filePath = storageService.store(file,uploadId);
        UploadFile uploadFile = UploadFile.builder()
                .id(uploadId)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .storagePath(filePath)
                .uploadTime(LocalDateTime.now())
                .expireTime(LocalDateTime.now().plusDays(7))
                .build();

        uploadMapper.insert(uploadFile);
        return uploadId;
    }
}
