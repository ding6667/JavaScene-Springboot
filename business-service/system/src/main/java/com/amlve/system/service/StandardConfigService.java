package com.amlve.system.service;

import com.amlve.system.dto.request.StandardConfigRequest;
import com.amlve.system.dto.request.StandardConfigUpdateRequest;
import com.amlve.system.dto.response.StandardConfigResponse;

import java.util.List;

public interface StandardConfigService {
    Integer update(StandardConfigRequest standardConfigRequest);
    StandardConfigResponse createStandardSettings(Long userId, StandardConfigRequest standardConfigRequest);
    List<StandardConfigResponse> getByUserId(Long userId);
    StandardConfigResponse updateStandard(Long id, StandardConfigUpdateRequest request);
    void deleteStandard(Long id);

}