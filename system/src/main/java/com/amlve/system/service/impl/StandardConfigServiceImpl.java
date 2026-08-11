package com.amlve.system.service.impl;


import com.amlve.system.converter.StandardConfigConverter;
import com.amlve.system.dto.StandardConfigRequest;
import com.amlve.systemapi.dto.StandardConfigDTO;
import com.amlve.common.exception.BusinessException;
import com.amlve.system.service.StandardConfigService;
import com.amlve.system.constant.ErrorMessageConstant;
import com.amlve.system.enumeration.UserConfigKeyEnum;
import com.amlve.system.mapper.SettingsMapper;
import com.amlve.system.mapper.StandardConfigMapper;
import com.amlve.system.tableEnt.StandardConfig;
import com.amlve.system.tableEnt.UserConfig;
import com.amlve.system.vo.StandardConfigResponse;
import com.amlve.systemapi.service.StandardService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class StandardConfigServiceImpl extends ServiceImpl<StandardConfigMapper,StandardConfig> implements StandardConfigService , StandardService {

    private final StandardConfigMapper standardConfigMapper;
    private final StandardConfigConverter standardConfigMapMapper;
    private final SettingsMapper userConfigMapper;

    @Override
    public StandardConfigDTO selectByName(String name) {
        LambdaQueryWrapper<StandardConfig> queryWrapper = Wrappers.<StandardConfig>lambdaQuery();
        queryWrapper.eq(StandardConfig::getStandardName, name);
        return standardConfigMapMapper.toDTO(standardConfigMapper.selectOne(queryWrapper));
    }

    @Override
    public StandardConfigDTO selectById(Long id) {
        return standardConfigMapMapper.toDTO(standardConfigMapper.selectById(id));
    }

    @Override
    public Integer update(StandardConfigRequest standardConfigDTO) {
        return standardConfigMapper.updateById(standardConfigMapMapper.toEntity(standardConfigDTO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StandardConfigResponse createStandardSettings(Long userId, StandardConfigRequest standardConfigDTO) {
        StandardConfig standardConfig = standardConfigMapper.selectOne(Wrappers.lambdaQuery(StandardConfig.class).eq(StandardConfig::getStandardName, standardConfigDTO.getStandardName()));
        if (standardConfig != null) {
            throw new BusinessException(ErrorMessageConstant.STANDARD_NAME_EXIST);
        }
        if(standardConfigDTO.getIsCurrent() == 1){
            standardConfigMapper.update( Wrappers.lambdaUpdate(StandardConfig.class)
                    .eq(StandardConfig::getIsCurrent, 1)
                    .eq(StandardConfig::getUserId, userId)
                    .set(StandardConfig::getIsCurrent, 0));
        }
        StandardConfig sc = null;
        int rows = standardConfigMapper.insert((sc = StandardConfig.builder()
                .standardName(standardConfigDTO.getStandardName())
                .standardContent(standardConfigDTO.getStandardContent())
                .isCurrent(standardConfigDTO.getIsCurrent())
                .userId(userId)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build()));
        if (rows != 1) {
            throw new BusinessException(ErrorMessageConstant.CREATE_FAILED);
        }
        UserConfig userConfig = UserConfig.builder()
                .userId(userId.toString())
                .configKey(UserConfigKeyEnum.STANDARD_CONFIG.getConfigKey())
                .configValue(sc.getId())
                .updateTime(LocalDateTime.now().toString())
                .build();
        userConfigMapper.insert(userConfig);
        return StandardConfigResponse.builder()
                .id(sc.getId())
                .standardName(standardConfigDTO.getStandardName())
                .standardContent(standardConfigDTO.getStandardContent())
                .isCurrent(standardConfigDTO.getIsCurrent())
                .userId(userId)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
    }
}
