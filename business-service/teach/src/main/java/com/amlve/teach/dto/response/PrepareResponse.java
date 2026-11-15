package com.amlve.teach.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 备课受理响应
 * <p>
 * 备课已改为异步：接口只校验模板与章节素材、建立 status=2 的备课记录，再把智能体调用
 * 和 PPT 转换投入后台线程，因此返回体里<b>没有</b>教案/重难点/习题/实训/PPT 路径——
 * 这些要等后台生成完成才存在。
 * <p>
 * 前端凭 {@link #recordId} 轮询 {@code GET /api/teach/prepare/record/detail?id=}
 * 取最终材料（status 2=生成中，1=成功，0=失败，失败原因在 errorMsg 里）。
 */
@Data
public class PrepareResponse {

    /** 章节ID（回显，便于前端关联） */
    private Long chapterId;

    /** 备课记录ID，前端据此轮询详情接口 */
    private Long recordId;

    /** 受理时的状态码，恒为 2（生成中） */
    private Integer status;

    /** 状态中文名 */
    private String statusDesc;

    /** 备课记录受理时间 */
    private LocalDateTime generateTime;
}
