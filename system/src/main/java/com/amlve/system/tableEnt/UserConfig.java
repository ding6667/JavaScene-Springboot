package com.amlve.system.tableEnt;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserConfig {
    @TableId(type = IdType.AUTO)
    Long id;
    Long userId;
    Integer configKey;

    String configValue;
    LocalDateTime updateTime;
    Integer deleted;
}
