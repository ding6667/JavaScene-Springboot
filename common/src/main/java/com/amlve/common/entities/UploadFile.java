package com.amlve.common.entities;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 上传文件记录表实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("upload_file")
@Builder
public class UploadFile {

    /**
     * 文件ID（即 uploadId，由程序生成 32 位 UUID）
     */
    @TableId(type = IdType.INPUT)
    private String id;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 服务器存储路径（相对或绝对路径）
     */
    private String storagePath;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 过期时间（用于定时清理）
     */
    private LocalDateTime expireTime;
}