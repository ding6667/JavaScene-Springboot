package com.amlve.system.dto.response;

import lombok.Data;

/**
 * 管理员扩展信息响应
 */
@Data
public class AdminInfoResponse {

    private Long userId;

    private String adminNo;

    /**
     * 权限级别：1=普通管理员，2=超级管理员
     */
    private Integer permissionLevel;

    private String department;
}
