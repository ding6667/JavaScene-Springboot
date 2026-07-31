package com.amlve.system.service;

import com.amlve.system.dto.StandardConfigDTO;
import com.amlve.system.vo.SettingsVO;
import com.amlve.system.vo.StandardConfigVO;

public interface SettingsService {
    SettingsVO getSettings(Long userId);


}
