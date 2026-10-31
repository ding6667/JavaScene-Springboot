package com.amlve.teach.service;

import com.amlve.teach.dto.response.PptDownloadData;

/**
 * 备课成果 PPT 业务接口（教师端，仅本人数据可下载）
 */
public interface PptService {

    /**
     * 按备课记录下载 PPT 文件，成功后下载计数自增。
     * <p>
     * pptx_url 为空（历史记录只生成了 Markdown）或文件已被清理时，若该记录的 pptx_md 有内容，
     * 会现场重建 pptx 文件、回填 pptx_url 并补落 teach_ppt 记录，本次请求直接返回文件；
     * 无法重建（没有 pptx_md / 转换工具不可用）仍按原语义抛业务异常，接口契约不变。
     *
     * @param recordId 备课记录ID（归属校验经 teach_prepare_record 实现）
     * @return 文件名与字节内容
     */
    PptDownloadData downloadPpt(Long recordId);
}
