package com.amlve.system.tableEnt;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色字典表。
 * <p>
 * 刻意不继承 BaseEntity：sys_role 没有 deleted 列，套上 @TableLogic 会让每次查询都带上
 * deleted=0 条件从而报 Unknown column
 */
@Data
@TableName("sys_role")
public class SysRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色编码，当前库里是 1001/1002/1003 这类业务编号，不参与鉴权
     */
    private String roleCode;

    /**
     * 角色名，鉴权用的权威值：Spring Security 侧对应 ROLE_&lt;roleName&gt;
     */
    private String roleName;

    /**
     * 中文描述，直接回给前端展示
     */
    private String roleDesc;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
