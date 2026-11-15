package com.amlve.systemapi.service;

import com.amlve.common.request.Result;
import com.amlve.common.web.feign.FeignConfig;
import com.amlve.systemapi.dto.api.UserBaseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 用户与班级的跨模块查询入口。其他模块不得直查 sys_user / sys_student_info 表
 */
@FeignClient(name = "system-service", contextId = "userApi", path = "/internal/system/user", configuration = FeignConfig.class)
public interface UserApi {

    /**
     * 按用户ID查基础信息，查不到返回 data = null，不报错
     */
    @PostMapping("/getUserById")
    Result<UserBaseDTO> getUserById(@RequestParam("userId") Long userId);

    /**
     * 批量查基础信息，用于列表组装
     */
    @PostMapping("/getUsersByIds")
    Result<List<UserBaseDTO>> getUsersByIds(@RequestBody List<Long> userIds);

    /**
     * 按学号查学生用户ID，查不到返回 data = null 由调用方判断
     */
    @PostMapping("/getUserIdByStudentNo")
    Result<Long> getUserIdByStudentNo(@RequestParam("studentNo") String studentNo);

    /**
     * 判断用户是否属于某班级
     */
    @PostMapping("/isUserInClass")
    Result<Boolean> isUserInClass(@RequestParam("userId") Long userId, @RequestParam("classId") Long classId);

    /**
     * 某班级的全部学生ID
     */
    @PostMapping("/getStudentIdsByClass")
    Result<List<Long>> getStudentIdsByClass(@RequestParam("classId") Long classId);
}
