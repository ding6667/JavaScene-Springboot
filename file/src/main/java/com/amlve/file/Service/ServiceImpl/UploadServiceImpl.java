package com.amlve.file.Service.ServiceImpl;

import com.amlve.common.PageResult;
import com.amlve.common.VO.UploadFileVO;
import com.amlve.common.Entities.TableEnt.UploadFile;
import com.amlve.file.Mapper.UploadMapper;
import com.amlve.file.Service.StorageService;
import com.amlve.file.Service.UploadService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadServiceImpl extends ServiceImpl<UploadMapper,UploadFile> implements UploadService{

    private final UploadMapper uploadMapper;
    private final StorageService storageService;


    @Value("${temp.zip}")
    private String tempZip;


    @Override
    public String uploadZip(MultipartFile file) throws IOException {
        // 生成随机uploadId 组装实体
        String uploadId = UUID.randomUUID().toString().replace("-", "");

        String filePath = storageService.store(file,uploadId);
        UploadFile uploadFile = new UploadFile();
        uploadFile.setId(uploadId);
        uploadFile.setFileName(file.getOriginalFilename());
        uploadFile.setFileSize(file.getSize());
        uploadFile.setStoragePath(filePath);
        uploadFile.setUploadTime(LocalDateTime.now());
        uploadFile.setExpireTime(LocalDateTime.now().plusDays(7));

        uploadMapper.insert(uploadFile);
        return uploadId;
    }

    /**
     * 返回历史记录
     * @param pageNum
     * @param pageSize
     * @param fileName
     * @param isAsc
     * @return
     */
    @Override
    public PageResult<UploadFileVO> records(Long pageNum, Long pageSize, String fileName, Boolean isAsc) {
        //分页对象
        Page<UploadFile> page = new Page<>(pageNum,pageSize);
        //条件构造器
        var wrapper = Wrappers.lambdaQuery(UploadFile.class);
        //查询字段
        wrapper.select(UploadFile::getFileName,UploadFile::getFileSize,UploadFile::getUploadTime,UploadFile::getStoragePath);
        //模糊查询
        wrapper.like(fileName != null && !fileName.isBlank() , UploadFile::getFileName,fileName);
        //升降序
        if(isAsc){
            wrapper.orderByAsc(UploadFile::getFileName);
        }else  {
            wrapper.orderByDesc(UploadFile::getFileName);
        }
        //执行分页查询
        IPage<UploadFile> pageData = baseMapper.selectPage(page,wrapper);
        List<UploadFileVO> voList = pageData.getRecords().stream().map(record ->
            UploadFileVO.builder()
                    .fileName(record.getFileName())
                    .fileSize(record.getFileSize())
                    .uploadTime(record.getUploadTime())
                    .expireTime(record.getExpireTime())
                    .build()
        ).toList();
        PageResult<UploadFileVO> pageResult = new PageResult<>();
        pageResult.setRecords(voList);
        pageResult.setTotal(pageData.getTotal());
        pageResult.setCurrent(pageData.getCurrent());
        pageResult.setSize(pageData.getSize());
        return pageResult;
    }
}
