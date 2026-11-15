package com.amlve.common.web.feign;

import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import feign.FeignException;
import feign.Response;
import feign.Util;
import feign.codec.Decoder;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Feign 返回类型直接写业务类型（List&lt;UserBaseDTO&gt; 而不是 Result&lt;List&lt;...&gt;&gt;），
 * 响应体里的 Result 外壳由这里剥掉；调用方代码不必再关心 data/code。
 */
public class ResultFeignDecoder implements Decoder {

    private static final String CODE_FIELD = "code";
    private static final String DATA_FIELD = "data";

    private final ObjectMapper objectMapper;

    public ResultFeignDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException, FeignException {
        if (response.body() == null) {
            return null;
        }
        byte[] body = Util.toByteArray(response.body().asInputStream());
        if (body.length == 0) {
            return null;
        }

        JsonNode root = objectMapper.readTree(body);
        boolean wrapped = root.has(CODE_FIELD) && root.has(DATA_FIELD);

        // 声明成 Result 的接口保持原样反序列化，不脱壳
        if (type instanceof Class && Result.class.isAssignableFrom((Class<?>) type)) {
            return objectMapper.readValue(body, objectMapper.constructType(type));
        }
        if (!wrapped) {
            // 提供方直接返回裸对象（如 boolean、ScoreEvaluateResponse），没有 Result 外壳
            return objectMapper.readValue(body, objectMapper.constructType(type));
        }

        JsonNode code = root.get(CODE_FIELD);
        if (code.isNull() || code.asInt() != 200) {
            // 沿用提供方的业务码，交给 common-web 的全局异常处理器，语义与单体时一致
            throw new BusinessException(code.asInt(), root.path("msg").asText());
        }
        JsonNode data = root.get(DATA_FIELD);
        if (data.isNull()) {
            return null;
        }
        JavaType javaType = objectMapper.constructType(type);
        return objectMapper.readValue(data.traverse(), javaType);
    }
}
