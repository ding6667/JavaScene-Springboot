package com.amlve.file.service;

public interface FileConversionService {
    String wordConvertToMd(String uploadId);

    byte[] mdConvertToWord(String content);
}
