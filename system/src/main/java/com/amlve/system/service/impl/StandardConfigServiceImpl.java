package com.amlve.system.service.impl;


import com.amlve.common.dto.StandardConfigDTO;
import com.amlve.common.service.StandardService;
import com.amlve.system.mapper.StandardConfigMapMapper;
import com.amlve.system.mapper.StandardConfigMapper;
import com.amlve.system.service.StandardConfigService;
import com.amlve.system.tableEnt.StandardConfig;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class StandardConfigServiceImpl implements StandardConfigService , StandardService {

    private final StandardConfigMapper standardConfigMapper;
    private final StandardConfigMapMapper standardConfigMapMapper;

    @Override
    public StandardConfigDTO selectByName(String name) {
        LambdaQueryWrapper<StandardConfig> queryWrapper = Wrappers.<StandardConfig>lambdaQuery();
        queryWrapper.eq(StandardConfig::getStandardName, name);
        return standardConfigMapMapper.toDTO(standardConfigMapper.selectOne(queryWrapper));
    }

    @Override
    public StandardConfigDTO selectById(String id) {
        return standardConfigMapMapper.toDTO(standardConfigMapper.selectById(id));
    }

    @Override
    public Integer update(StandardConfigDTO standardConfigDTO) {
        return standardConfigMapper.updateById(standardConfigMapMapper.toEntity(standardConfigDTO));
    }
}
