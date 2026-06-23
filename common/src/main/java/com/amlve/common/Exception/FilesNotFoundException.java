package com.amlve.common.Exception;


import com.amlve.common.Constant.ErrorCodeConstant;

/**
 * 文件未找到异常
 * 当需要读取的文件不存在时抛出
 */
public class FilesNotFoundException extends BusinessException {

    /**
     * 使用默认错误码（FILE_NOT_FOUND）和自定义消息
     */
    public FilesNotFoundException(String message) {
        super(ErrorCodeConstant.FILE_NOT_FOUND, message);
    }

    /**
     * 使用默认错误码和消息，并包装原始异常
     */
    public FilesNotFoundException(String message, Throwable cause) {
        super(ErrorCodeConstant.FILE_NOT_FOUND, message, cause);
    }

    /**
     * 自定义错误码和消息（如果需要特殊处理）
     */
    public FilesNotFoundException(Integer code, String message) {
        super(code, message);
    }

    /**
     * 自定义错误码、消息和原始异常
     */
    public FilesNotFoundException(Integer code, String message, Throwable cause) {
        super(code, message, cause);
    }
}