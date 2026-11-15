package com.amlve.exam.util;

import com.amlve.common.utils.SecurityUtils;
import org.springframework.stereotype.Component;

/**
 * 考评模块统一鉴权支持。
 * <p>
 * 角色判定已由 JWT 过滤器装入 authorities（每请求从库加载，改角色即时生效），
 * 因此教师/管理员限制一律走类级 {@code @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")}，
 * 本类只保留「取当前登录用户」这一件事，不再重复查库比对角色。
 */
@Component
public class ExamAuthSupport {

    /**
     * 校验当前用户已登录，返回其用户 ID
     *
     * @return 当前登录用户 ID
     */
    public Long requireLogin() {
        return SecurityUtils.getUserId();
    }
}
