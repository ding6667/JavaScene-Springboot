package com.amlve.dialogue.tableEnt;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@TableName("intent_draft")
@Builder
public class IntentDraft {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String draftIntentType;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> draftTemplateText;
    private String originalUserQuery;
    private Integer status; // 0: 待审核, 1: 已审核, 2: 已拒绝
    private LocalDateTime createdTime;
    private LocalDateTime reviewedTime;
}
