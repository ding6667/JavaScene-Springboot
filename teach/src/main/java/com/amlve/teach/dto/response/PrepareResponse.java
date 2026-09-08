package com.amlve.teach.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 返回给前端的教案生成结果
 */
@Data
public class PrepareResponse {

    /** 章节ID（回显，便于前端关联） */
    private Long chapterId;

    /** 教案内容文本 */
    private String teachingPlan;

    /** 教案生成时间 */
    private LocalDateTime generateTime;
}
