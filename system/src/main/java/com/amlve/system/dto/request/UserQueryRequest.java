package com.amlve.system.dto.request;

import lombok.Data;

/**
 * 用户列表分页查询请求（管理端）
 */
@Data
public class UserQueryRequest {

    /**
     * 角色筛选（可选）：1=学生，2=教师，3=管理员
     */
    private Integer roleId;

    /**
     * 账号状态筛选（可选）：0=禁用，1=启用
     */
    private Integer enabled;

    /**
     * 用户名 / 姓名 / 手机号模糊关键词
     */
    private String keyword;

    /**
     * 班级筛选（可选，学生场景）
     */
    private Long classId;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
