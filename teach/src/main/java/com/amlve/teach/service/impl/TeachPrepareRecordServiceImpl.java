package com.amlve.teach.service.impl;

import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.common.utils.FileFormatUtil;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.knowledgeapi.dto.response.ChapterNameDTO;
import com.amlve.knowledgeapi.service.KnowledgeChapterApi;
import com.amlve.teach.dto.response.LessonPlanExportData;
import com.amlve.teach.dto.response.PrepareRecordDetailResponse;
import com.amlve.teach.dto.response.PrepareRecordListResponse;
import com.amlve.teach.mapper.TeachPrepareRecordMapper;
import com.amlve.teach.service.TeachPrepareRecordService;
import com.amlve.teach.tableEnt.TeachPrepareRecord;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 备课记录查询与教案导出业务实现。
 * <p>
 * 归属校验：所有查询强制 teacher_id = 当前登录教师，越权访问抛业务异常。
 * 教案导出：Markdown 经 common 的 FileFormatUtil（python 脚本）转 Word，
 * 导出文件同时落盘留档（失败仅告警不阻断）。
 */
@Slf4j
@Service
public class TeachPrepareRecordServiceImpl implements TeachPrepareRecordService {

    @Resource
    private TeachPrepareRecordMapper teachPrepareRecordMapper;

    @Resource
    private FileFormatUtil fileFormatUtil;

    @Resource
    private KnowledgeChapterApi knowledgeChapterApi;

    /** 生成文件存储根目录（与 PPT 转换输出目录保持一致） */
    @Value("${temp.commonFile}")
    private String commonFileDir;

    @Override
    public PrepareRecordListResponse listMyRecords(Long chapterId, Integer pageNum, Integer pageSize) {
        Long teacherId = SecurityUtils.getUserId();
        // 分页查询本人成功记录，可选按章节过滤，按备课时间倒序
        Page<TeachPrepareRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TeachPrepareRecord> wrapper = new LambdaQueryWrapper<TeachPrepareRecord>()
                .eq(TeachPrepareRecord::getTeacherId, teacherId)
                .eq(TeachPrepareRecord::getStatus, 1)
                .eq(chapterId != null, TeachPrepareRecord::getChapterId, chapterId)
                .orderByDesc(TeachPrepareRecord::getCreateTime);
        Page<TeachPrepareRecord> result = teachPrepareRecordMapper.selectPage(page, wrapper);

        PrepareRecordListResponse response = new PrepareRecordListResponse();
        response.setTotal(result.getTotal());
        response.setPageNum(result.getCurrent());
        response.setPageSize(result.getSize());
        response.setRecords(result.getRecords().stream().map(this::toListItem).collect(Collectors.toList()));
        return response;
    }

    /** 列表项转换：仅携带是否有值标记，不含材料全文 */
    private PrepareRecordListResponse.Item toListItem(TeachPrepareRecord record) {
        PrepareRecordListResponse.Item item = new PrepareRecordListResponse.Item();
        item.setId(record.getId());
        item.setChapterId(record.getChapterId());
        item.setCreateTime(record.getCreateTime());
        item.setHasLessonPlan(StringUtils.hasText(record.getLessonPlan()));
        item.setHasKeyPoints(StringUtils.hasText(record.getKeyPoints()));
        item.setHasHomework(StringUtils.hasText(record.getHomework()));
        item.setHasLab(StringUtils.hasText(record.getLab()));
        item.setHasPpt(StringUtils.hasText(record.getPptxUrl()));
        return item;
    }

    @Override
    public PrepareRecordDetailResponse getRecordDetail(Long recordId) {
        TeachPrepareRecord record = requireOwnedRecord(recordId);

        PrepareRecordDetailResponse response = new PrepareRecordDetailResponse();
        response.setId(record.getId());
        response.setChapterId(record.getChapterId());
        response.setLessonPlan(record.getLessonPlan());
        response.setKeyPoints(record.getKeyPoints());
        response.setHomework(record.getHomework());
        response.setLab(record.getLab());
        response.setPptxMd(record.getPptxMd());
        response.setPptxUrl(record.getPptxUrl());
        // templateId 待 teach_ppt / teach_prepare_record 增加 template_id 列后落库，当前降级返回 null
        response.setTemplateId(null);
        response.setCreateTime(record.getCreateTime());
        return response;
    }

    @Override
    public LessonPlanExportData exportLessonPlan(Long recordId) {
        TeachPrepareRecord record = requireOwnedRecord(recordId);
        if (!StringUtils.hasText(record.getLessonPlan())) {
            throw new BusinessException("教案内容为空，无法导出");
        }

        // Markdown 转 Word 字节（common 的 FileFormatUtil，依赖 python 脚本执行环境）
        byte[] wordBytes = fileFormatUtil.generateWord(record.getLessonPlan());

        // 落盘留档 {temp.commonFile}/{yyyy}/{MM}/{dd}/{UUID}.docx，失败不影响导出
        try {
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path outputDir = Paths.get(commonFileDir, dateDir);
            Files.createDirectories(outputDir);
            Files.write(Paths.get(outputDir.toString(), UUID.randomUUID() + ".docx"), wordBytes);
        } catch (Exception e) {
            log.warn("教案留档写入失败，不影响本次导出: recordId={}", recordId, e);
        }

        LessonPlanExportData data = new LessonPlanExportData();
        data.setFileName(buildLessonPlanFileName(record));
        data.setContent(wordBytes);
        log.info("教案导出成功: recordId={}, teacherId={}, bytes={}", recordId, record.getTeacherId(), wordBytes.length);
        return data;
    }

    /**
     * 教案导出文件名：{章节名}_教案.docx（章节名取 knowledge-api 短名），
     * 章节查询失败或名为空时降级为 备课教案_{recordId}.docx；非法文件名字符统一替换为下划线
     */
    private String buildLessonPlanFileName(TeachPrepareRecord record) {
        String fallback = "备课教案_" + record.getId() + ".docx";
        try {
            Result<ChapterNameDTO> result = knowledgeChapterApi.getChapterNameById(record.getChapterId());
            if (result == null || result.getCode() != 200 || result.getData() == null) {
                return fallback;
            }
            String name = result.getData().getShortName();
            if (!StringUtils.hasText(name)) {
                name = result.getData().getChapterName();
            }
            if (!StringUtils.hasText(name)) {
                return fallback;
            }
            // 文件名非法字符替换，防止落盘/下载失败
            String safe = name.replaceAll("[\\\\/:*?\"<>|\\s]", "_").trim();
            return safe + "_教案.docx";
        } catch (Exception e) {
            // 章节名查询异常：降级保底文件名，不阻塞导出
            log.warn("教案文件名章节查询异常，降级保底: recordId={}, err={}", record.getId(), e.getMessage());
            return fallback;
        }
    }

    /**
     * 查询并校验备课记录归属：仅本人记录可访问
     */
    private TeachPrepareRecord requireOwnedRecord(Long recordId) {
        TeachPrepareRecord record = teachPrepareRecordMapper.selectById(recordId);
        if (record == null || !SecurityUtils.getUserId().equals(record.getTeacherId())) {
            throw new BusinessException("备课记录不存在或无权访问");
        }
        return record;
    }
}
