package com.amlve.system.service;

import com.amlve.system.dto.StandardConfigRequest;
import com.amlve.system.vo.StandardConfigResponse;

public interface StandardConfigService {
    Integer update(StandardConfigRequest standardConfigRequest);
    StandardConfigResponse createStandardSettings(Long userId, StandardConfigRequest standardConfigRequest);

}
