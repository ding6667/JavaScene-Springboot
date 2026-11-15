package com.amlve.system.dto.request;

import lombok.Data;

/**
 * 登出请求。整个 body 可省略，缺省只登出当前 token。
 */
@Data
public class LogoutRequest {

    /**
     * true = 同时作废该用户更早签发的所有 token（退出所有设备）
     */
    private Boolean allDevices = false;
}
