package com.amlve.teach.dto.response;

import lombok.Data;

/**
 * PPT 下载数据（内部传输用，不经 Controller 直接对外）
 */
@Data
public class PptDownloadData {

    /** 下载文件名（含扩展名 .pptx） */
    private String fileName;

    /** PPT 文件字节内容 */
    private byte[] content;
}
