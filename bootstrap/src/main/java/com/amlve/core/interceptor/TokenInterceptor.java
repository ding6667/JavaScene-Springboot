package com.amlve.core.interceptor;

import com.amlve.common.utils.JWTUtil;
import com.amlve.common.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class TokenInterceptor implements HandlerInterceptor {


    private final JWTUtil jwtUtil;

    // 放行地址（不需要登录的接口：登录、注册、验证码）
    private static final String[] WHITE_LIST = {
            "/api/login",
            "/api/register"
    };

    /**
     * 请求进入controller之前执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        // 白名单直接放行
        for (String path : WHITE_LIST) {
            if (uri.contains(path)) {
                return true;
            }
        }

        // 获取请求头Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }
        String token = authHeader.substring(7);

        // 校验token合法性
        if (!jwtUtil.verifyToken(token)) {
            response.setStatus(401);
            return false;
        }

        // 解析userId，存入线程上下文
        Long userId = jwtUtil.getUserIdByToken(token);
        UserContext.setUserId(userId);
        return true;
    }

    /**
     * 视图渲染完成后执行（接口项目基本不用）
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {

    }

    /**
     * 请求完全结束，无论是否异常都会执行 → 清理上下文【核心】
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}