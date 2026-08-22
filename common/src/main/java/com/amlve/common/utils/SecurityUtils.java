package com.amlve.common.utils;
import com.amlve.common.security.UserIdAuthenticationToken;
import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.constant.ErrorMessageConstant;
import com.amlve.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    /**
     * 获取当前登录用户 ID
     */
    public static Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCodeConstant.UNAUTHORIZED, ErrorMessageConstant.UNAUTHORIZED_MESSAGE);
        }

        if (authentication instanceof UserIdAuthenticationToken) {
            return ((UserIdAuthenticationToken) authentication).getUserId();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCodeConstant.UNAUTHORIZED, ErrorMessageConstant.UNAUTHORIZED_MESSAGE);
            }
        }
        throw new BusinessException(ErrorCodeConstant.UNAUTHORIZED, ErrorMessageConstant.UNAUTHORIZED_MESSAGE);
    }

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }
}