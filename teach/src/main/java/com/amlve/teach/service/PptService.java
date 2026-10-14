package com.amlve.teach.service;

import com.amlve.teach.dto.response.PptDownloadData;

/**
 * 备课成果 PPT 业务接口（教师端，仅本人数据可下载）
 */
public interface PptService {

    /**
     * 按备课记录下载 PPT 文件，成功后下载计数自增
     *
     * @param recordId 备课记录ID（归属校验经 teach_prepare_record 实现）
     * @return 文件名与字节内容
     */
    PptDownloadData downloadPpt(Long recordId);
}
