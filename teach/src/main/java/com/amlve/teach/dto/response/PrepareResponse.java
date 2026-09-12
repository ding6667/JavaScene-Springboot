package com.amlve.teach.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 返回给前端的备课材料生成结果
 */
@Data
public class PrepareResponse {

    /** 章节ID（回显，便于前端关联） */
    private Long chapterId;

    /** 教案内容（Markdown） */
    private String lessonPlan;

    /** PPT 文件路径（转换失败或未开启时为 null） */
    private String pptxUrl;

    /** 重难点清单（Markdown） */
    private String keyPoints;

    /** 课后习题（Markdown） */
    private String homework;

    /** 上机实训方案（Markdown） */
    private String lab;

    /** 备课记录ID（落库后回填，供前端查询/追溯） */
    private Long recordId;

    /** 备课材料生成时间 */
    private LocalDateTime generateTime;
}
