package com.amlve.system.service.impl;

import com.amlve.system.dto.CommonSettingsRequest;
import com.amlve.system.enumeration.UserConfigKeyEnum;
import com.amlve.system.mapper.SettingsMapper;
import com.amlve.system.mapper.StandardConfigMapper;
import com.amlve.system.service.SettingsService;
import com.amlve.system.tableEnt.UserConfig;
import com.amlve.system.vo.SettingsResponse;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SettingsServiceImpl extends ServiceImpl<SettingsMapper, UserConfig> implements SettingsService {
    private final SettingsMapper settingsMapper;
    private final StandardConfigMapper standardConfigMapper;

    @Override
    public SettingsResponse getCommonSettings(Long userId) {
        List<UserConfig> userConfigs = settingsMapper.selectList(Wrappers.lambdaQuery(UserConfig.class).eq(UserConfig::getUserId, userId));
        return getSettingsResponse(userId, userConfigs);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettingsResponse updateCommonSettings(Long userId, CommonSettingsRequest settingsRequest) {
        List<UserConfig> userConfigs = settingsMapper.selectList(Wrappers.lambdaQuery(UserConfig.class).eq(UserConfig::getUserId, userId));
        UserConfig existing = userConfigs.stream()
                .filter(uc -> uc.getConfigKey().equals(settingsRequest.getConfigKey()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setConfigValue(settingsRequest.getConfigValue());
            existing.setUpdateTime(LocalDateTime.now());
            settingsMapper.updateById(existing);
        } else {
            settingsMapper.insert(UserConfig.builder()
                    .userId(userId)
                    .configKey(settingsRequest.getConfigKey())
                    .configValue(settingsRequest.getConfigValue())
                    .build());
        }
        return getSettingsResponse(userId, userConfigs);
    }

    private SettingsResponse getSettingsResponse(Long userId, List<UserConfig> userConfigs) {
        List<SettingsResponse.Setting> settings = new ArrayList<>();
        userConfigs.forEach(userConfig -> {
            settings.add(new SettingsResponse.Setting(userConfig.getConfigKey()
                    , userConfig.getConfigValue()
                    , Objects.requireNonNull(UserConfigKeyEnum.fromConfigKey(userConfig.getConfigKey())).getDesc()));
        });
        return SettingsResponse.builder()
                .userId(userId)
                .settings(settings)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettingsResponse defaultSettings(Long userId) {
        settingsMapper.delete(
                Wrappers.lambdaQuery(UserConfig.class)
                        .eq(UserConfig::getUserId, userId)
        );

        List<UserConfig> defaults = Arrays.stream(UserConfigKeyEnum.values())
                .map(enumVal -> UserConfig.builder()
                        .userId(userId)
                        .configKey(enumVal.getConfigKey())
                        .configValue(enumVal.getDefaultValue())
                        .build())
                .toList();

        settingsMapper.insert(defaults);

        List<UserConfig> updatedList = settingsMapper.selectList(
                Wrappers.lambdaQuery(UserConfig.class)
                        .eq(UserConfig::getUserId, userId)
        );
        return getSettingsResponse(userId, updatedList);
    }


}
