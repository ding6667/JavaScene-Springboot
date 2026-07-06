package com.amlve.common.VO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 上传文件记录表VO
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("upload_file")
@Builder
public class UploadFileVO {
    /**
     * id
     */
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
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 过期时间（用于定时清理）
     */
    private LocalDateTime expireTime;
}