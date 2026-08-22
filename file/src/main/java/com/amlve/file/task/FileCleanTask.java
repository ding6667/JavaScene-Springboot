package com.amlve.file.task;

import com.amlve.common.exception.BusinessException;
import com.amlve.file.constant.ErrorMessageConstant;
import com.amlve.file.mapper.UploadMapper;
import com.amlve.file.tableEnt.UploadFile;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanTask {

    private final UploadMapper uploadFileMapper;
    /**
     * 每天凌晨0点执行清理过期文件任务
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanFiles() {
        log.info("开始清理过期文件");
        LambdaQueryWrapper<UploadFile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.le(UploadFile::getExpireTime, LocalDateTime.now());
        List<UploadFile> uploadFiles = uploadFileMapper.selectList(queryWrapper);
        uploadFiles.forEach(uploadFile -> {
            uploadFile.setExpired(1);
            uploadFileMapper.updateById(uploadFile);
            try {
                Files.delete(Paths.get(uploadFile.getRawStoragePath()));
                Files.delete(Paths.get(uploadFile.getUnzipStoragePath()));
            } catch (IOException e) {
                throw new BusinessException(ErrorMessageConstant.DELETE_FAILED);
            }
            log.info("删除文件：{}", uploadFile);
        });
    }
}
