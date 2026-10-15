package com.amlve.teach.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 备课记录详情响应（教师端，含五类材料全文）
 */
@Data
public class PrepareRecordDetailResponse {

    /** 备课记录ID */
    private Long id;

    /** 关联教材章节ID */
    private Long chapterId;

    /** 教案内容（Markdown） */
    private String lessonPlan;

    /** 重难点清单（Markdown） */
    private String keyPoints;

    /** 课后习题（Markdown） */
    private String homework;

    /** 上机实训方案（Markdown） */
    private String lab;

    /** PPT原始Markdown内容 */
    private String pptxMd;

    /** PPT文件路径 */
    private String pptxUrl;

    /** 生成使用的PPT模板ID（待 teach_ppt 增加 template_id 列后落库，当前降级返回 null） */
    private Long templateId;

    /** 备课时间 */
    private LocalDateTime createTime;
}
