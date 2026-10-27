package com.amlve.systemapi.service;

import com.amlve.common.request.Result;
import com.amlve.systemapi.dto.api.UserBaseDTO;

import java.util.List;

/**
 * 用户与班级的跨模块查询入口。其他模块不得直查 sys_user / sys_student_info 表
 */
public interface UserApi {

    /**
     * 按用户ID查基础信息，查不到返回 data = null，不报错
     */
    Result<UserBaseDTO> getUserById(Long userId);

    /**
     * 批量查基础信息，用于列表组装
     */
    Result<List<UserBaseDTO>> getUsersByIds(List<Long> userIds);

    /**
     * 按学号查学生用户ID，查不到返回 data = null 由调用方判断
     */
    Result<Long> getUserIdByStudentNo(String studentNo);

    /**
     * 判断用户是否属于某班级
     */
    Result<Boolean> isUserInClass(Long userId, Long classId);

    /**
     * 某班级的全部学生ID
     */
    Result<List<Long>> getStudentIdsByClass(Long classId);
}
