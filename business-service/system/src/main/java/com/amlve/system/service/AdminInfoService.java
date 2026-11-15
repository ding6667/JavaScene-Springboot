package com.amlve.system.service;

import com.amlve.system.dto.response.AdminInfoResponse;

/**
 * 管理员履历扩展信息。系统当前仅一名管理员，不提供自助修改入口
 */
public interface AdminInfoService {

    AdminInfoResponse getByUserId(Long userId);

    void createEmpty(Long userId);
}
