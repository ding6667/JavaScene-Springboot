package com.amlve.common.request;

import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.constant.ErrorMessageConstant;
import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public Result() {}

    public Result(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCodeConstant.SUCCESS, ErrorMessageConstant.SUCCESS_MSG, data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ErrorCodeConstant.SUCCESS, message, data);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(ErrorCodeConstant.ERROR, message);
    }


}
