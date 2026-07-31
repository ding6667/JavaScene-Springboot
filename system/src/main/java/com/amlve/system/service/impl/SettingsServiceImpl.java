package com.amlve.system.service.impl;

import com.amlve.common.exception.BusinessException;
import com.amlve.system.constant.ErrorMessageConstant;
import com.amlve.system.dto.StandardConfigDTO;
import com.amlve.system.mapper.SettingsMapper;
import com.amlve.system.mapper.StandardConfigMapper;
import com.amlve.system.service.SettingsService;
import com.amlve.system.service.StandardConfigService;
import com.amlve.system.tableEnt.StandardConfig;
import com.amlve.system.tableEnt.UserConfig;
import com.amlve.system.vo.SettingsVO;
import com.amlve.system.vo.StandardConfigVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class SettingsServiceImpl extends ServiceImpl<SettingsMapper, UserConfig> implements SettingsService {
    private final SettingsMapper settingsMapper;
    private final StandardConfigMapper standardConfigMapper;

    @Override
    public SettingsVO getSettings(Long userId) {
        List<UserConfig> userConfigs = settingsMapper.selectList(Wrappers.lambdaQuery(UserConfig.class).eq(UserConfig::getUserId, userId));
        Map<String, Object> settings = new HashMap<>();
        userConfigs.forEach(userConfig -> {
            settings.put(userConfig.getConfigKey(), userConfig.getConfigValue());
        });
        return SettingsVO.builder()
                .userId(userId)
                .settings(settings)
                .build();
    }


}
