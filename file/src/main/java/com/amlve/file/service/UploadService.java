package com.amlve.file.service;

import com.amlve.common.request.PageResult;
import com.amlve.file.vo.UploadFileVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface UploadService {

    String uploadFile(MultipartFile file, String fileType) throws IOException;

    PageResult<UploadFileVO> records(Long pageNum, Long pageSize, String fileName, Boolean isAsc,String fileType);

    Integer delete(String id);
    Map<String, Object> loadAsResource(String id);
}
