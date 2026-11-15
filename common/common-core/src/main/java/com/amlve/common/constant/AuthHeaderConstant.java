package com.amlve.common.constant;

/**
 * 网关与下游服务之间的身份请求头。网关解析 JWT 后写入，下游凭它重建 SecurityContext，
 * 因此这三个头必须在网关入口一律剥离，防止客户端伪造。
 */
public final class AuthHeaderConstant {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_NAME = "X-User-Name";
    public static final String USER_ROLES = "X-User-Roles";
    public static final String REQUEST_ID = "REQUEST-ID";

    /** 跨服务内部调用前缀，只注册给 Feign，不对外由网关路由 */
    public static final String INTERNAL_PATH = "/internal";

    private AuthHeaderConstant() {
    }
}
