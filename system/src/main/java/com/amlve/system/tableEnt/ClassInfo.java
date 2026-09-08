package com.amlve.system.tableEnt;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("class")
public class ClassInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String className;

    private String classCode;

    private Integer grade;

    private Long userId;

    /**
     * 所属课程ID
     */
    private Long courseId;

    private String semester;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}