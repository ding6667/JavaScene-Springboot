package com.amlve.system.mapper;

import com.amlve.system.tableEnt.UserConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SettingsMapper extends BaseMapper<UserConfig> {
}