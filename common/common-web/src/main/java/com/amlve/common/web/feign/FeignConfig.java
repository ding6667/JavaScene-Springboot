package com.amlve.common.web.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;

/**
 * 每个 @FeignClient 通过 configuration = FeignConfig.class 单独引用。
 * 故意不加 @Configuration、也不放在任何被扫描的包里，避免污染全部 Feign 客户端。
 */
public class FeignConfig {

    @Bean
    public Decoder feignDecoder(ObjectMapper objectMapper) {
        return new ResultFeignDecoder(objectMapper);
    }

    /**
     * 包住默认 SpringEncoder：非 multipart 请求仍走原 JSON 编码，
     * multipart（如 QAAgentClient.createDocumentByFile）由 feign-form 编码文件与表单字段。
     */
    @Bean
    public Encoder feignEncoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        return new SpringFormEncoder(new SpringEncoder(messageConverters));
    }
}
