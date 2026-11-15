package com.amlve.system.service;

import com.amlve.system.dto.request.CommonSettingsRequest;
import com.amlve.system.dto.response.SettingsResponse;

public interface SettingsService {
    SettingsResponse getCommonSettings(Long userId);


    SettingsResponse updateCommonSettings(Long userId, CommonSettingsRequest settingsRequest);

    SettingsResponse defaultSettings(Long userId);
}