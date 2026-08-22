package com.amlve.file.service.serviceImpl;

import com.amlve.common.exception.BusinessException;
import com.amlve.file.constant.ErrorMessageConstant;
import com.amlve.file.mapper.UploadMapper;
import com.amlve.file.service.StorageService;
import com.amlve.file.service.UploadService;
import com.amlve.file.tableEnt.UploadFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@Slf4j
public class StorageServiceImpl implements StorageService {


    @Value("${temp.zip:./temp/zip}")
    private String tempZip;
    @Value("${user.dir}/temp/commonFile")
    private String tempCommonFile;


    /**
     * 存储文件
     * @param file 文件对象
     * @param fileId 文件ID
     * @return 文件路径字符串
     * @throws IOException
     */
    @Override
    public String store(MultipartFile file, String fileId) throws IOException {
        // 按日期生成文件夹
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException(ErrorMessageConstant.FILE_NAME_INVALID);
        }
        int dotIndex = originalFilename.lastIndexOf(".");
        String fileType = (dotIndex == -1) ? "" : originalFilename.substring(dotIndex);
        Path basePath = switch (fileType) {
            case ".zip" -> Paths.get(tempZip).resolve(dir);
            default -> Paths.get(tempCommonFile).resolve(dir);
        };

        Files.createDirectories(basePath);
        // 生成唯一文件名
        String fileName = fileId + fileType;
        Path targetPath = basePath.resolve(fileName);
        file.transferTo(targetPath);

        return targetPath.toString();

    }

    @Override
    public Integer delete(String filePath) {
        try {
            Files.delete(Path.of(filePath));
            return 1;
        } catch (IOException e) {
            log.error("删除文件失败", e);
            return 0;

        }

    }

}
