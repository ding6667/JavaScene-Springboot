package com.amlve.teach.service.impl;

import com.amlve.common.constant.ErrorMessageConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.teach.dto.response.PptDownloadData;
import com.amlve.teach.mapper.TeachPptMapper;
import com.amlve.teach.mapper.TeachPrepareRecordMapper;
import com.amlve.teach.service.PptService;
import com.amlve.teach.tableEnt.TeachPpt;
import com.amlve.teach.tableEnt.TeachPrepareRecord;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 备课成果 PPT 业务实现。
 * <p>
 * 归属校验：teach_ppt 表无 teacher_id 列，归属校验经 teach_prepare_record（teacher_id + pptx_url）实现，
 * 即只能下载本人备课记录关联的 PPT。
 * 下载计数：成功读取文件后按 file_path 匹配原子自增，防并发丢失。
 */
@Slf4j
@Service
public class PptServiceImpl implements PptService {

    @Resource
    private TeachPrepareRecordMapper teachPrepareRecordMapper;

    @Resource
    private TeachPptMapper teachPptMapper;

    @Override
    public PptDownloadData downloadPpt(Long recordId) {
        // 1. 经备课记录校验归属（teach_ppt 无 teacher_id 列）
        TeachPrepareRecord record = teachPrepareRecordMapper.selectById(recordId);
        if (record == null || !SecurityUtils.getUserId().equals(record.getTeacherId())) {
            throw new BusinessException(ErrorMessageConstant.PREPARE_RECORD_NOT_FOUND);
        }
        if (!StringUtils.hasText(record.getPptxUrl())) {
            throw new BusinessException(ErrorMessageConstant.PREPARE_PPT_NOT_GENERATED);
        }

        // 2. 文件存在性校验（生成文件可能已被清理）
        String filePath = record.getPptxUrl();
        if (!Files.exists(Paths.get(filePath))) {
            throw new BusinessException(ErrorMessageConstant.PPT_FILE_EXPIRED);
        }

        // 3. 读取文件内容
        byte[] content;
        try {
            content = Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            log.error("PPT文件读取失败: recordId={}, path={}", recordId, filePath, e);
            throw new BusinessException(ErrorMessageConstant.PPT_FILE_READ_FAILED);
        }

        // 4. 下载计数 +1（setSql 原子自增，避免并发下计数丢失）
        teachPptMapper.update(null, new LambdaUpdateWrapper<TeachPpt>()
                .eq(TeachPpt::getFilePath, filePath)
                .setSql("download_count = download_count + 1"));

        // 5. 文件名优先取 teach_ppt.file_name，查不到用 recordId 兜底
        TeachPpt ppt = teachPptMapper.selectOne(new LambdaQueryWrapper<TeachPpt>()
                .eq(TeachPpt::getFilePath, filePath)
                .last("LIMIT 1"));
        PptDownloadData data = new PptDownloadData();
        data.setFileName(ppt != null && StringUtils.hasText(ppt.getFileName())
                ? ppt.getFileName() : "课件_" + recordId + ".pptx");
        data.setContent(content);
        log.info("PPT下载成功: recordId={}, teacherId={}, bytes={}", recordId, record.getTeacherId(), content.length);
        return data;
    }
}
