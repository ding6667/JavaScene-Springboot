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

    /** 是否有 PPT 正文（pptx_md 非空）：为 true 即可点击下载，文件没转出来时下载接口会现场补生成 */
    private Boolean hasPptMd;

    /**
     * PPT 三态：0=无课件内容，1=有正文·文件未产出，2=文件就绪（与列表接口口径一致）
     */
    private Integer pptStatus;

    /**
     * PPT文件路径。
     * <p>
     * 仅在文件真实存在（pptStatus=2）时返回；异步转换未完成或文件已被清理时返回 null，
     * 不把会 404 的死链给前端——前端此时应按 hasPptMd/pptStatus 展示「待生成」并调下载接口触发补生成。
     */
    private String pptxUrl;

    /** 生成使用的PPT模板ID（待 teach_ppt 增加 template_id 列后落库，当前降级返回 null） */
    private Long templateId;

    /** 状态（1=成功，0=失败，2=生成中），前端据此判断轮询是否结束 */
    private Integer status;

    /** 状态中文名 */
    private String statusDesc;

    /** 失败原因（status=0 时有值） */
    private String errorMsg;

    /** 备课时间 */
    private LocalDateTime createTime;
}
