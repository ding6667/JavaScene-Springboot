package com.amlve.file.service.serviceImpl;

import com.amlve.common.dto.UploadFileDTO;
import com.amlve.common.request.PageResult;
import com.amlve.common.utils.UserContext;
import com.amlve.file.converter.UploadFileMapMapper;
import com.amlve.file.vo.UploadFileVO;
import com.amlve.file.tableEnt.UploadFile;
import com.amlve.file.mapper.UploadMapper;
import com.amlve.file.service.StorageService;
import com.amlve.file.service.UploadService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
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
public class UploadServiceImpl extends ServiceImpl<UploadMapper,UploadFile> implements UploadService , com.amlve.common.service.UploadService {

    private final UploadMapper uploadMapper;
    private final StorageService storageService;
    private final UploadFileMapMapper uploadFileMapMapper;

    @Value("${temp.zip}")
    private String tempZip;


    @Override
    public String uploadZip(MultipartFile file) throws IOException {
        // 生成随机uploadId 组装实体
        String uploadId = UUID.randomUUID().toString().replace("-", "");

        String filePath = storageService.store(file,uploadId);
        UploadFile uploadFile = new UploadFile();
        uploadFile.setId(uploadId);
        uploadFile.setUserId(UserContext.getUserId());
        uploadFile.setFileName(file.getOriginalFilename());
        uploadFile.setFileSize(file.getSize());
        uploadFile.setRawStoragePath(filePath);
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
        wrapper.select(UploadFile::getFileName,UploadFile::getFileSize,UploadFile::getUploadTime,UploadFile::getRawStoragePath);
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

    @Override
    public Integer delete(String id) {
        int deleteInTable = uploadMapper.deleteById(id);
        String path = uploadMapper.selectById(id).getRawStoragePath();
        Integer deleteFile = storageService.delete(path);
        return deleteInTable > 0 && deleteFile > 0 ? 1 : 0;
    }

    @Override
    public UploadFileDTO selectById(String id) {
        return uploadFileMapMapper.toDto(uploadMapper.selectById(id));
    }

    @Override
    public Integer updateById(UploadFileDTO uploadFileDTO) {
        return uploadMapper.updateById(uploadFileMapMapper.toEntity(uploadFileDTO));
    }
}
