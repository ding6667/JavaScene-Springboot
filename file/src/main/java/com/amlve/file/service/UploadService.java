package com.amlve.file.service;

import com.amlve.common.request.PageResult;
import com.amlve.file.vo.UploadFileVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UploadService {

    String uploadFile(MultipartFile file) throws IOException;

    PageResult<UploadFileVO> records(Long pageNum, Long pageSize, String fileName, Boolean isAsc);

    Integer delete(String id);
}
