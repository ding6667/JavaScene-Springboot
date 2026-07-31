package com.amlve.system.service;

import com.amlve.common.dto.StandardConfigDTO;
import com.amlve.system.vo.StandardConfigVO;

public interface StandardConfigService {
    Integer update(StandardConfigDTO standardConfigDTO);
    StandardConfigVO createStandardSettings(Long userId, com.amlve.system.dto.StandardConfigDTO standardConfigDTO);

}
