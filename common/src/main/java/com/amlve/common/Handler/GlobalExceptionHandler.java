package com.amlve.common.Handler;


import com.amlve.common.Constant.ErrorCodeConstant;
import com.amlve.common.Exception.BusinessException;
import com.amlve.common.Exception.FilesNotFoundException;
import com.amlve.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理所有异常，返回规范的 Result 格式
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 业务异常 ====================

    /**
     * 未见失踪异常
     * @param ex
     * @return
     */
    @ExceptionHandler(FilesNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleFilesNotFoundException(FilesNotFoundException ex) {
        log.error("文件未找到,message = {}" ,ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }
    /**
     * 自定义业务异常（如参数校验失败、业务状态不允许等）
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    // ==================== 参数校验异常 ====================

    /**
     * 校验 @RequestBody 参数（Spring Validation）
     * 例如：@Valid 标注的实体类校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return Result.error(ErrorCodeConstant.PARAM_INVALID, message);
    }

    /**
     * 校验 @RequestParam 参数（Spring Validation）
     * 例如：@RequestParam @NotBlank String name
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return Result.error(ErrorCodeConstant.PARAM_INVALID, message);
    }

    /**
     * 参数绑定异常（例如：参数类型不匹配）
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", message);
        return Result.error(ErrorCodeConstant.PARAM_INVALID, message);
    }

    /**
     * 缺少必要的请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("缺少必要参数: {}", e.getParameterName());
        return Result.error(ErrorCodeConstant.PARAM_MISSING, "缺少必要参数: " + e.getParameterName());
    }

    /**
     * 请求体格式错误（例如：JSON 格式不正确）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误: {}", e.getMessage());
        return Result.error(ErrorCodeConstant.PARAM_INVALID, "请求体格式错误");
    }

    // ==================== 文件上传异常 ====================

    /**
     * 文件大小超限
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Result<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件大小超限: {}", e.getMessage());
        return Result.error(ErrorCodeConstant.FILE_SIZE_EXCEEDED, "文件大小超过限制（最大50MB）");
    }

    // ==================== 请求方式异常 ====================

    /**
     * 请求方法不支持（如 GET 请求用了 POST）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return Result.error(ErrorCodeConstant.METHOD_NOT_ALLOWED, "请求方法不支持: " + e.getMethod());
    }

    /**
     * 不支持的 MediaType（如 Content-Type 不正确）
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<Void> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.warn("不支持的 Content-Type: {}", e.getMessage());
        return Result.error(ErrorCodeConstant.UNSUPPORTED_MEDIA_TYPE, "不支持的 Content-Type: " + e.getContentType());
    }

    // ==================== 404 异常 ====================

    /**
     * 找不到接口
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("接口不存在: {}", e.getRequestURL());
        return Result.error(ErrorCodeConstant.NOT_FOUND, "接口不存在: " + e.getRequestURL());
    }

    // ==================== 兜底异常 ====================

    /**
     * 所有未捕获的异常（兜底）
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.error(ErrorCodeConstant.SYSTEM_ERROR, "系统异常，请稍后重试");
    }
}