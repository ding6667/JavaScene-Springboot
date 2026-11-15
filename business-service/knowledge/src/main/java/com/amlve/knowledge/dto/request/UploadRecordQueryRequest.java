package com.amlve.knowledge.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库上传记录分页查询条件
 * <p>
 * 所有筛选条件均可选；上传人不传时由服务端按角色决定（管理员可查全部，教师强制本人）。
 */
@Data
public class UploadRecordQueryRequest {

    /**
     * 按上传记录ID精确查（可选）
     * <p>
     * 上传已改异步受理，接口只回 recordId；前端凭它轮询本列表取终态，
     * 没有这个条件就得翻页找，所以补上。数据隔离照旧：教师传别人的 recordId 仍会被 uploadBy 挡掉。
     */
    private Long recordId;

    /**
     * 章节ID（可选）
     */
    private Long chapterId;

    /**
     * 状态筛选：0=处理中，1=成功，2=失败
     */
    private Integer status;

    /**
     * 上传人ID（仅管理员可用；教师调用时该字段被忽略）
     */
    private Long uploadBy;

    /**
     * 上传时间下界（含）
     */
    private LocalDateTime startTime;

    /**
     * 上传时间上界（含）
     */
    private LocalDateTime endTime;

    /**
     * 页码，从 1 开始
     */
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 10;
}
