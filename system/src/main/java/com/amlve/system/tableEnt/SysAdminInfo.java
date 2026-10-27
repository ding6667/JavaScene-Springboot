package com.amlve.system.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理员信息扩展表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_admin_info")
public class SysAdminInfo extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("admin_no")
    private String adminNo;

    /**
     * 权限级别：1=普通管理员，2=超级管理员
     */
    @TableField("permission_level")
    private Integer permissionLevel;

    @TableField("department")
    private String department;
}
