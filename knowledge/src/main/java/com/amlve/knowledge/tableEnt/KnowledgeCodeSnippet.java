package com.amlve.knowledge.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_code_snippet")
public class KnowledgeCodeSnippet extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("chapter_id")
    private Long chapterId;

    private String title;

    private String code;

    @TableField("is_key_example")
    private Boolean isKeyExample;

    private Integer page;

    @TableField("page_order")
    private Integer pageOrder;
}