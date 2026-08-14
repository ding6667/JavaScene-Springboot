package com.amlve.file.tableEnt;

import com.amlve.file.enumeration.FileType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 上传文件记录表实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("upload_file")
public class UploadFile {

    /**
     * 文件ID（即 uploadId，由程序生成 32 位 UUID）
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private Long userId;
    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 上传源文件路径
     */
    private String rawStoragePath;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 过期时间（用于定时清理）
     */
    private LocalDateTime expireTime;
    /**
     * 解压后的文件路径
     */
    private String unzipStoragePath;
    /**
     * 文件类型
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private FileType fileType;

    //是否删除：0-未删除 1-已删除
    private Integer deleted;
}