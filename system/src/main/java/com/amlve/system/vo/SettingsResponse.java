package com.amlve.system.vo;

import com.amlve.system.tableEnt.UserConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingsResponse {

    private Long userId;
    private List<Setting> settings;
    public static record Setting(Integer configKey, String configValue ,String configKeyName) {}

}
