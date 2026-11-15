package com.amlve.common.util.config;

import com.amlve.common.utils.FileFormatUtil;
import com.amlve.common.utils.ZipUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 公共工具 Bean。FileFormatUtil 依赖本机 Python 与脚本路径，
 * 只有配置了 python.script.* 的服务（目前是 teach）才装配，避免其他服务因缺配置起不来。
 */
@AutoConfiguration
public class UtilInfraAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ZipUtil zipUtil() {
        return new ZipUtil();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("python.script.md_to_docx_path")
    public FileFormatUtil fileFormatUtil() {
        return new FileFormatUtil();
    }
}
