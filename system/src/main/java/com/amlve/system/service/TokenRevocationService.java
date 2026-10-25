package com.amlve.system.service;

/**
 * JWT 吊销存储。JWT 本身无状态，登出与强制下线靠 Redis 补一层可撤销的记录。
 * <p>
 * 只实现两种粒度：单 token 拉黑（{@code auth:blacklist:{jti}}）与用户级全端下线
 * （{@code auth:kicked:{userId}}）。同端互踢需要 clientId 维度，项目已废弃端标识概念，不做。
 */
public interface TokenRevocationService {

    /**
     * 登出，幂等：token 缺失、伪造、已过期都正常返回，不做吊销也不报错。
     *
     * @param authorizationHeader 原始 Authorization 头，可为 null
     * @param allDevices          是否同时作废该用户更早签发的所有 token
     */
    void logout(String authorizationHeader, boolean allDevices);

    /**
     * 使指定用户在当前时刻之前签发的所有 token 失效。
     * 改密码等「旧凭证不应再被信任」的变更必须调用，否则旧 token 仍能用到自然过期。
     *
     * @param reason 记入日志的原因标识
     */
    void revokeAllTokens(Long userId, String reason);

    /**
     * 供 JWT 过滤器在装载 authorities 之前判断 token 是否已被吊销。
     *
     * @param jti                 token 唯一标识
     * @param userId              token 所属用户
     * @param issuedAtEpochSecond token 签发时刻（秒），早于用户下线时刻即失效
     */
    boolean isRevoked(String jti, Long userId, long issuedAtEpochSecond);
}
