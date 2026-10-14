package com.amlve.teach.dto.response;

import lombok.Data;

/**
 * 教案 Word 导出数据（内部传输用，不经 Controller 直接对外）
 */
@Data
public class LessonPlanExportData {

    /** 下载文件名（含扩展名 .docx） */
    private String fileName;

    /** Word 文件字节内容 */
    private byte[] content;
}
