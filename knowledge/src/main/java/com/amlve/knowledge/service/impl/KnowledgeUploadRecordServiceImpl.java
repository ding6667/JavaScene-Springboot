package com.amlve.knowledge.service.impl;

import com.amlve.agentqaapi.dto.response.DifyDocumentVO;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.PageResult;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.knowledge.constant.ErrorMessageConstant;
import com.amlve.knowledge.dto.request.UploadRecordQueryRequest;
import com.amlve.knowledge.dto.response.KnowledgeUploadRecordVO;
import com.amlve.knowledge.enumeration.UploadStatusEnum;
import com.amlve.knowledge.enumeration.UploadTypeEnum;
import com.amlve.knowledge.mapper.KnowledgeUploadRecordMapper;
import com.amlve.knowledge.tableEnt.KnowledgeUploadRecord;
import com.amlve.knowledge.service.KnowledgeUploadRecordService;
import com.amlve.systemapi.service.UserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库上传留痕服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeUploadRecordServiceImpl implements KnowledgeUploadRecordService {

    /**
     * 落库的失败原因长度上限（对齐 error_msg 列宽 varchar(500)）：
     * 库里存尽量完整的原因，截断只发生在列表页展示时，否则全量原因永久丢失
     */
    private static final int ERROR_MSG_PERSIST_MAX_LENGTH = 500;

    /**
     * 列表页展示的失败原因长度上限（文档 4.7.4：详情再返回全量）
     */
    private static final int ERROR_MSG_MAX_LENGTH = 200;

    /**
     * file_name 列宽 varchar(255)、file_type 列宽 varchar(20)：
     * 中文长文件名很容易超 255，insert 抛异常会让整次上传失败且无痕迹，故入库前钳制
     */
    private static final int FILE_NAME_MAX_LENGTH = 255;

    private static final int FILE_TYPE_MAX_LENGTH = 20;

    private final KnowledgeUploadRecordMapper uploadRecordMapper;

    private final UserService userService;

    /**
     * REQUIRES_NEW：外层上传事务回滚时，"处理中"记录也必须留下，否则失败的上传查不到痕迹
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public KnowledgeUploadRecord start(String datasetId, Long chapterId, String chapterName,
                                       String fileName, Long fileSize, String fileType, Integer uploadType) {
        KnowledgeUploadRecord record = new KnowledgeUploadRecord();
        record.setDatasetId(datasetId);
        record.setChapterId(chapterId);
        record.setChapterName(chapterName);
        record.setFileName(truncate(fileName, FILE_NAME_MAX_LENGTH));
        record.setFileSize(fileSize);
        record.setFileType(truncate(fileType, FILE_TYPE_MAX_LENGTH));
        record.setUploadType(uploadType != null ? uploadType : UploadTypeEnum.CREATE.getCode());
        record.setStatus(UploadStatusEnum.PROCESSING.getCode());
        record.setTotalSegments(0);
        record.setCompletedSegments(0);
        record.setUploadBy(SecurityUtils.getUserId());
        record.setUploadByName(SecurityUtils.getCurrentUsername());
        record.setUploadTime(LocalDateTime.now());
        uploadRecordMapper.insert(record);
        return record;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markSuccess(KnowledgeUploadRecord record, DifyDocumentVO document, Integer uploadType) {
        if (record == null || record.getId() == null || document == null) {
            return;
        }
        record.setDocumentId(document.getDocumentId());
        record.setBatch(document.getBatch());
        record.setStatus(UploadStatusEnum.SUCCESS.getCode());
        record.setUploadType(uploadType);
        record.setTotalSegments(document.getTotalSegments());
        record.setCompletedSegments(document.getCompletedSegments());
        record.setFinishTime(LocalDateTime.now());
        assertUpdated(uploadRecordMapper.updateById(record), record.getId());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markFailed(KnowledgeUploadRecord record, String documentId, String batch, String errorMsg) {
        if (record == null || record.getId() == null) {
            // 记录本身没建成功（如建档前就抛异常），无从回写
            return;
        }
        try {
            record.setStatus(UploadStatusEnum.FAILED.getCode());
            // 失败行必须带上已知的 documentId/batch：否则"哪个 Dify 文档可能残留"无从查起
            record.setDocumentId(documentId);
            record.setBatch(batch);
            record.setErrorMsg(truncate(errorMsg, ERROR_MSG_PERSIST_MAX_LENGTH));
            record.setFinishTime(LocalDateTime.now());
            assertUpdated(uploadRecordMapper.updateById(record), record.getId());
        } catch (Exception e) {
            // 旁路写入失败不能顶掉主流程的原始异常，否则前端看到的是 SQL 错误而非上传失败原因
            log.error("上传留痕回写失败: recordId={}, 原始失败={}", record.getId(), errorMsg, e);
        }
    }

    /**
     * 回写影响 0 行说明记录行对本事务不可见——通常是 start() 的 REQUIRES_NEW 被人改回 REQUIRED，
     * 那一行还挂在外层未提交事务里。这种情况 MySQL 不报错，只会静默丢痕迹，故显式告警。
     */
    private void assertUpdated(int rows, Long recordId) {
        if (rows == 0) {
            log.error("上传留痕回写影响 0 行，请检查 start() 是否仍为 REQUIRES_NEW: recordId={}", recordId);
        }
    }

    @Override
    public PageResult<KnowledgeUploadRecordVO> listRecords(UploadRecordQueryRequest request) {
        Long currentUserId = SecurityUtils.getUserId();
        boolean admin = userService.isAdmin(currentUserId);
        if (!admin && !userService.isTeacher(currentUserId)) {
            log.warn("权限不足: userId={} 尝试查询上传记录", currentUserId);
            throw new BusinessException(ErrorMessageConstant.PERMISSION_DENIED_TEACHER_OR_ADMIN);
        }

        Page<KnowledgeUploadRecord> page = new Page<>(request.getPageNum(), request.getPageSize());
        // 教师只能看自己的上传记录，管理员看全部；不接受前端传 userId 越权查询
        Long uploadBy = admin ? request.getUploadBy() : currentUserId;
        IPage<KnowledgeUploadRecord> pageData = uploadRecordMapper.selectPage(page,
                Wrappers.<KnowledgeUploadRecord>lambdaQuery()
                        .eq(request.getRecordId() != null, KnowledgeUploadRecord::getId, request.getRecordId())
                        .eq(request.getChapterId() != null, KnowledgeUploadRecord::getChapterId, request.getChapterId())
                        .eq(request.getStatus() != null, KnowledgeUploadRecord::getStatus, request.getStatus())
                        .eq(uploadBy != null, KnowledgeUploadRecord::getUploadBy, uploadBy)
                        .ge(request.getStartTime() != null, KnowledgeUploadRecord::getUploadTime, request.getStartTime())
                        .le(request.getEndTime() != null, KnowledgeUploadRecord::getUploadTime, request.getEndTime())
                        .orderByDesc(KnowledgeUploadRecord::getUploadTime));

        List<KnowledgeUploadRecordVO> list = pageData.getRecords().stream()
                .map(this::toVo)
                .collect(Collectors.toList());

        PageResult<KnowledgeUploadRecordVO> result = new PageResult<>();
        result.setRecords(list);
        result.setTotal(pageData.getTotal());
        result.setCurrent(pageData.getCurrent());
        result.setSize(pageData.getSize());
        return result;
    }

    /**
     * 实体转列表 VO：状态与类型走枚举翻译，耗时按上传与完成时间之差计算
     */
    private KnowledgeUploadRecordVO toVo(KnowledgeUploadRecord record) {
        KnowledgeUploadRecordVO vo = new KnowledgeUploadRecordVO();
        vo.setId(record.getId());
        vo.setChapterId(record.getChapterId());
        vo.setChapterName(record.getChapterName());
        vo.setFileName(record.getFileName());
        vo.setFileSize(record.getFileSize());
        vo.setFileType(record.getFileType());
        vo.setUploadType(record.getUploadType());
        vo.setUploadTypeDesc(UploadTypeEnum.descOf(record.getUploadType()));
        vo.setStatus(record.getStatus());
        vo.setStatusDesc(UploadStatusEnum.descOf(record.getStatus()));
        vo.setTotalSegments(record.getTotalSegments());
        vo.setDocumentId(record.getDocumentId());
        vo.setBatch(record.getBatch());
        vo.setErrorMsg(truncate(record.getErrorMsg(), ERROR_MSG_MAX_LENGTH));
        vo.setUploadByName(record.getUploadByName());
        vo.setUploadTime(record.getUploadTime());
        vo.setFinishTime(record.getFinishTime());
        if (record.getUploadTime() != null && record.getFinishTime() != null) {
            vo.setDurationSeconds(Duration.between(record.getUploadTime(), record.getFinishTime()).getSeconds());
        }
        return vo;
    }

    private String truncate(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
