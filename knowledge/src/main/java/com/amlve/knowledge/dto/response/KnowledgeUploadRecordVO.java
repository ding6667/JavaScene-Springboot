package com.amlve.knowledge.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库上传记录列表项
 * <p>
 * 状态与类型同时返回码值和中文描述，前端无需再维护一份映射表。
 */
@Data
public class KnowledgeUploadRecordVO {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 关联章节ID
     */
    private Long chapterId;

    /**
     * 章节名称
     */
    private String chapterName;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 上传类型码值：1=新增，2=更新
     */
    private Integer uploadType;

    /**
     * 上传类型中文描述
     */
    private String uploadTypeDesc;

    /**
     * 状态码值：0=处理中，1=成功，2=失败
     */
    private Integer status;

    /**
     * 状态中文描述
     */
    private String statusDesc;

    /**
     * 生成的分段总数（0 表示空壳文档）
     */
    private Integer totalSegments;

    /**
     * Dify 文档ID（失败时也回填，管理员据此核对知识库是否残留孤儿文档）
     */
    private String documentId;

    /**
     * Dify 批次标识
     */
    private String batch;

    /**
     * 失败原因（列表页截断 200 字符）
     */
    private String errorMsg;

    /**
     * 上传人
     */
    private String uploadByName;

    /**
     * 上传发起时间
     */
    private LocalDateTime uploadTime;

    /**
     * 处理完成时间
     */
    private LocalDateTime finishTime;

    /**
     * 处理耗时（秒），由上传时间与完成时间之差算出
     */
    private Long durationSeconds;
}
