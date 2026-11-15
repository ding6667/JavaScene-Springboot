package com.amlve.system.service;

import com.amlve.system.tableEnt.SysRole;

import java.util.Map;

/**
 * 角色字典查询。sys_role 是角色的唯一权威来源：
 * 授权用的角色名、展示用的中文描述、新增用户时的合法性校验全部走这里，
 * 不再在 sys_user 上冗余 role_name，也不再在代码里硬编码角色ID
 */
public interface RoleService {

    /**
     * sys_role 中必须存在同名行的三个语义角色名。业务代码只在这里定义一次，
     * 新增角色插一行 sys_role 即可，不需要改枚举或硬编码ID
     */
    String ADMIN = "ADMIN";

    String TEACHER = "TEACHER";

    String STUDENT = "STUDENT";

    /**
     * 按角色ID取角色，取不到直接拒绝，不允许兜底成某个默认角色
     */
    SysRole require(Long roleId);

    /**
     * 鉴权用的角色名（不含 ROLE_ 前缀）
     */
    String nameOf(Long roleId);

    /**
     * 展示用的中文描述
     */
    String descOf(Long roleId);

    /**
     * 按角色名反查角色ID，用于「给学生建档」「保留管理员」这类需要按角色定位的业务规则
     */
    Long idOf(String roleName);

    /**
     * 一次取全量角色按ID索引，供列表组装使用，避免逐行查字典
     */
    Map<Long, SysRole> allById();

    /**
     * 角色语义判定收口在这里：业务代码不再散落 "ADMIN" 这类字面量，
     * 也不再依赖代码里硬编码的角色ID
     */
    boolean isAdmin(Integer roleId);

    boolean isTeacher(Integer roleId);

    boolean isStudent(Integer roleId);
}
