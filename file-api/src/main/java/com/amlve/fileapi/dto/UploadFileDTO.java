package com.amlve.fileapi.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UploadFileDTO {
    private String id;
    private String fileName; Long fileSize;
    private String rawStoragePath;
    private LocalDateTime uploadTime;
    private LocalDateTime expireTime;
    private String unzipStoragePath;
}
