package com.amlve.teach.service;

import com.amlve.teach.dto.response.LessonPlanExportData;
import com.amlve.teach.dto.response.PrepareRecordDetailResponse;
import com.amlve.teach.dto.response.PrepareRecordListResponse;

/**
 * 备课记录查询与教案导出业务接口（教师端，仅本人数据可见）
 */
public interface TeachPrepareRecordService {

    /**
     * 分页查询本人备课成功记录（可选按章节过滤）
     *
     * @param chapterId 章节ID（可空）
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @return 分页记录列表
     */
    PrepareRecordListResponse listMyRecords(Long chapterId, Integer pageNum, Integer pageSize);

    /**
     * 查询备课记录详情（五类材料全文）
     *
     * @param recordId 备课记录ID
     * @return 详情
     */
    PrepareRecordDetailResponse getRecordDetail(Long recordId);

    /**
     * 导出备课记录的教案为 Word 文件字节
     *
     * @param recordId 备课记录ID
     * @return 文件名与字节内容
     */
    LessonPlanExportData exportLessonPlan(Long recordId);
}
