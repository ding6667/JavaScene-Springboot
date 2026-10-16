package com.amlve.knowledge.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 知识库上传记录（每次上传一行，成功失败都留痕）
 * <p>
 * 与 knowledge_content 的区别：本表是操作流水（含失败与清理结果），
 * knowledge_content 是"文档→章节"的正式映射，只有校验通过的文档才会写入后者。
 * <p>
 * 主键保留自增：本表是流水表，不承担跨库关联，无需像 knowledge_* 业务表那样用雪花ID。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_upload_record")
public class KnowledgeUploadRecord extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Dify 知识库ID
     */
    @TableField("dataset_id")
    private String datasetId;

    /**
     * Dify 文档ID（建档成功后填充；建档即失败时为空）
     */
    @TableField("document_id")
    private String documentId;

    /**
     * Dify 批次标识，用于查询索引状态
     */
    private String batch;

    /**
     * 关联章节ID
     */
    @TableField("chapter_id")
    private Long chapterId;

    /**
     * 章节名称（冗余，便于列表查询不再 JOIN）
     */
    @TableField("chapter_name")
    private String chapterName;

    /**
     * 原始文件名（文本上传时为文档名）
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 文件类型（txt/md/pdf/docx 等扩展名）
     */
    @TableField("file_type")
    private String fileType;

    /**
     * 上传类型：1=新增，2=更新，取值见 UploadTypeEnum
     */
    @TableField("upload_type")
    private Integer uploadType;

    /**
     * 状态：0=处理中，1=成功，2=失败，取值见 UploadStatusEnum
     */
    private Integer status;

    /**
     * 生成的分段总数，0 表示空壳文档
     */
    @TableField("total_segments")
    private Integer totalSegments;

    /**
     * 已完成的分段数
     */
    @TableField("completed_segments")
    private Integer completedSegments;

    /**
     * 失败原因（成功时为空）
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 上传人ID（关联 sys_user.id）
     */
    @TableField("upload_by")
    private Long uploadBy;

    /**
     * 上传人账号（system-api 未提供真实姓名查询，此处存登录名以保证可审计）
     */
    @TableField("upload_by_name")
    private String uploadByName;

    /**
     * 上传发起时间
     */
    @TableField("upload_time")
    private LocalDateTime uploadTime;

    /**
     * 处理完成时间（成功或失败的落库时刻，与 upload_time 之差即耗时）
     */
    @TableField("finish_time")
    private LocalDateTime finishTime;
}
