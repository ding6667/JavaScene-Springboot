package com.amlve.system.tableEnt;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserConfig {
    @TableId(type = IdType.AUTO)
    Long id;
    String userId;
    String configKey;

    @TableField(typeHandler = JacksonTypeHandler.class)
    Object configValue;
    String updateTime;
    Integer deleted;
}
