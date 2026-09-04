package com.amlve.fileapi.dto.api;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UploadFileDTO {
    private String id;
    private String fileName;
    private Long fileSize;
    private String rawStoragePath;
    private LocalDateTime uploadTime;
    private LocalDateTime expireTime;
    private String unzipStoragePath;
    private String fileType;
    private Long classId;
}