package com.amlve.common.dto;

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
