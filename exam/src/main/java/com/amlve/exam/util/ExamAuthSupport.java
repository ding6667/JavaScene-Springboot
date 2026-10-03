package com.amlve.exam.util;

import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.systemapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 考评模块统一鉴权支持。
 * <p>
 * 项目 JWT 不携带角色信息，{@code @PreAuthorize("hasRole(...)")} 运行时取不到角色会静默失败，
 * 因此考评接口统一采用编程式查库鉴权：管理类接口要求教师/管理员，学生类接口要求已登录。
 */
@Component
@RequiredArgsConstructor
public class ExamAuthSupport {

    private final UserService userService;

    /**
     * 校验当前用户为教师或管理员，不满足时抛出无权限异常
     */
    public void requireTeacherOrAdmin() {
        Long userId = SecurityUtils.getUserId();
        if (!userService.isTeacher(userId) && !userService.isAdmin(userId)) {
            throw new BusinessException(ErrorCodeConstant.ACCESS_DENIED, "权限不足，仅管理员或教师可操作");
        }
    }

    /**
     * 校验当前用户已登录，返回其用户 ID
     *
     * @return 当前登录用户 ID
     */
    public Long requireLogin() {
        return SecurityUtils.getUserId();
    }
}
