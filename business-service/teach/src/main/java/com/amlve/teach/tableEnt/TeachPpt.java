package com.amlve.teach.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * PPT文件记录表实体，对应数据库 teach_ppt 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("teach_ppt")
public class TeachPpt extends BaseEntity {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联教材章节ID */
    @TableField("chapter_id")
    private Long chapterId;

    /** 文件名（含UUID） */
    @TableField("file_name")
    private String fileName;

    /** 文件存储路径 */
    @TableField("file_path")
    private String filePath;

    /** PPT原始Markdown内容 */
    @TableField("md_content")
    private String mdContent;

    /** 文件大小（字节） */
    @TableField("file_size")
    private Long fileSize;

    /** 下载次数 */
    @TableField("download_count")
    private Integer downloadCount;
}
