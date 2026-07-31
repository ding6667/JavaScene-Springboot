package com.amlve.system.tableEnt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserConfig {
    Long id;
    String userId;
    String configKey;
    String configValue;
    String updateTime;
    Integer deleted;
}
