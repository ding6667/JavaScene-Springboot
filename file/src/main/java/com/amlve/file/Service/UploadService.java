package com.amlve.file.Service;

import com.amlve.common.PageResult;
import com.amlve.common.VO.UploadFileVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UploadService {

    String uploadZip(MultipartFile file) throws IOException;

    PageResult<UploadFileVO> records(Long pageNum, Long pageSize, String fileName, Boolean isAsc);

    Integer delete(String id);
}
