package com.amlve.system.util;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 角色 → 登录后落地页的唯一决策点。新增角色或调整路由只改这里，前端不再按角色写 if-else。
 */
@Component
public class RouteResolver {

    /**
     * 无角色或未知角色时的落地页
     */
    private static final String FALLBACK_ROUTE = "/403";

    /**
     * 多角色时的入口优先级，从高到低
     */
    private static final List<String> ROLE_PRIORITY = List.of("ADMIN", "TEACHER", "STUDENT");

    private static final Map<String, String> ROLE_ROUTES = Map.of(
            "ADMIN", "/admin/dashboard",
            "TEACHER", "/teacher/dashboard",
            "STUDENT", "/student/dashboard"
    );

    /**
     * @param roles 不含 ROLE_ 前缀的角色名列表
     * @return 命中的第一个高优先级角色对应的路由，无命中返回 {@link #FALLBACK_ROUTE}
     */
    public String resolve(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return FALLBACK_ROUTE;
        }
        for (String role : ROLE_PRIORITY) {
            if (roles.contains(role)) {
                return ROLE_ROUTES.get(role);
            }
        }
        return FALLBACK_ROUTE;
    }
}
