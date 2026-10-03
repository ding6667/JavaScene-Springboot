package com.amlve.system.service.impl;


import com.amlve.common.request.Result;
import com.amlve.system.converter.StandardConfigConverter;
import com.amlve.system.dto.request.StandardConfigRequest;
import com.amlve.system.dto.request.StandardConfigUpdateRequest;
import com.amlve.systemapi.dto.api.StandardConfigResponse;
import com.amlve.common.exception.BusinessException;
import com.amlve.system.service.StandardConfigService;
import com.amlve.system.constant.ErrorCodeConstant;
import com.amlve.system.constant.ErrorMessageConstant;
import com.amlve.system.mapper.SettingsMapper;
import com.amlve.system.mapper.StandardConfigMapper;
import com.amlve.system.tableEnt.StandardConfig;
import com.amlve.systemapi.service.StandardService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StandardConfigServiceImpl extends ServiceImpl<StandardConfigMapper,StandardConfig> implements StandardConfigService , StandardService {

    private final StandardConfigMapper standardConfigMapper;
    private final StandardConfigConverter standardConfigMapMapper;
    private final SettingsMapper userConfigMapper;

    @Override
    public Result<StandardConfigResponse> selectByName(String name) {
        LambdaQueryWrapper<StandardConfig> queryWrapper = Wrappers.<StandardConfig>lambdaQuery();
        queryWrapper.eq(StandardConfig::getStandardName, name);
        return Result.success(standardConfigMapMapper.toDTO(standardConfigMapper.selectOne(queryWrapper)));
    }

    @Override
    public Result<StandardConfigResponse> selectById(Long id) {
        return Result.success(standardConfigMapMapper.toDTO(standardConfigMapper.selectById(id)));
    }

    @Override
    public Integer update(StandardConfigRequest standardConfigDTO) {
        return standardConfigMapper.updateById(standardConfigMapMapper.toEntity(standardConfigDTO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public com.amlve.system.dto.response.StandardConfigResponse createStandardSettings(Long userId, StandardConfigRequest standardConfigDTO) {
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

        return com.amlve.system.dto.response.StandardConfigResponse.builder()
                .id(sc.getId())
                .standardName(standardConfigDTO.getStandardName())
                .standardContent(standardConfigDTO.getStandardContent())
                .isCurrent(standardConfigDTO.getIsCurrent())
                .userId(userId)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
    }

    @Override
    public List<com.amlve.system.dto.response.StandardConfigResponse> getByUserId(Long userId) {
        LambdaQueryWrapper<StandardConfig> queryWrapper = Wrappers.<StandardConfig>lambdaQuery()
                .eq(StandardConfig::getUserId, userId)
                .eq(StandardConfig::getDeleted, 0);
        List<StandardConfig> configs = standardConfigMapper.selectList(queryWrapper);
        return configs.stream().map(config -> com.amlve.system.dto.response.StandardConfigResponse.builder()
                .id(config.getId())
                .standardName(config.getStandardName())
                .standardContent(config.getStandardContent())
                .isCurrent(config.getIsCurrent())
                .userId(config.getUserId())
                .createTime(config.getCreateTime())
                .updateTime(config.getUpdateTime())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public com.amlve.system.dto.response.StandardConfigResponse updateStandard(Long id, StandardConfigUpdateRequest request) {
        StandardConfig existing = standardConfigMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new BusinessException(ErrorCodeConstant.BUSINESS_ERROR, ErrorMessageConstant.STANDARD_NOT_FOUND);
        }
        existing.setStandardName(request.getStandardName());
        existing.setStandardContent(request.getStandardContent());
        existing.setIsCurrent(request.getIsCurrent());
        existing.setUpdateTime(LocalDateTime.now());
        int rows = standardConfigMapper.updateById(existing);
        if (rows != 1) {
            throw new BusinessException(ErrorCodeConstant.BUSINESS_ERROR, ErrorMessageConstant.UPDATE_FAILED);
        }
        return com.amlve.system.dto.response.StandardConfigResponse.builder()
                .id(existing.getId())
                .standardName(existing.getStandardName())
                .standardContent(existing.getStandardContent())
                .isCurrent(existing.getIsCurrent())
                .userId(existing.getUserId())
                .createTime(existing.getCreateTime())
                .updateTime(existing.getUpdateTime())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStandard(Long id) {
        StandardConfig existing = standardConfigMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new BusinessException(ErrorCodeConstant.BUSINESS_ERROR, ErrorMessageConstant.STANDARD_NOT_FOUND);
        }
        existing.setDeleted(1);
        existing.setUpdateTime(LocalDateTime.now());
        int rows = standardConfigMapper.updateById(existing);
        if (rows != 1) {
            throw new BusinessException(ErrorCodeConstant.BUSINESS_ERROR, ErrorMessageConstant.DELETE_FAILED);
        }
    }
}