package com.amlve.system.tableEnt;
import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user")
public class User extends BaseEntity {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 登录账号（唯一）
     */
    private String username;

    /**
     * 密码（BCrypt加密）
     */
    private String password;

    /**
     * 真实姓名（教师姓名）
     */
    private String realName;

    /**
     * 邮箱（用于找回密码）
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 账号状态：0-禁用，1-启用
     */
    private Integer enabled;


    private Integer roleId;

    private String roleName;
}
