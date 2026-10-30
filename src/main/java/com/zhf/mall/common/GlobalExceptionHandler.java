package com.zhf.mall.common;

import com.zhf.mall.exception.BusinessException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.NonTransientDataAccessResourceException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 将业务异常和框架异常统一转换为 code、msg、data 三字段响应。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** 处理校验失败、用户名重复等可预期的业务错误。 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException exception) {
        ResultCode resultCode = exception.getResultCode();
        return ResponseEntity.status(resultCode.getCode())
                .body(Result.error(resultCode, exception.getMessage()));
    }

    /** 处理 Controller 中 @Valid 触发的请求参数校验错误。 */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String msg = exception.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(Objects::nonNull)
                .sorted()
                .findFirst()
                .orElse(ResultCode.BAD_REQUEST.getMsg());
        return buildResponse(ResultCode.BAD_REQUEST.getCode(), msg, headers);
    }

    /** 拒绝空请求体、畸形 JSON 和字段类型错误。 */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return buildResponse(ResultCode.INVALID_JSON.getCode(), ResultCode.INVALID_JSON.getMsg(), headers);
    }

    /** 保留 Spring MVC 的真实 HTTP 状态，并替换其默认返回体。 */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String msg = switch (status.value()) {
            case 400 -> ResultCode.BAD_REQUEST.getMsg();
            case 401 -> ResultCode.UNAUTHORIZED.getMsg();
            case 403 -> ResultCode.FORBIDDEN.getMsg();
            case 404 -> ResultCode.NOT_FOUND.getMsg();
            case 405 -> ResultCode.METHOD_NOT_ALLOWED.getMsg();
            case 406 -> ResultCode.NOT_ACCEPTABLE.getMsg();
            case 409 -> ResultCode.CONFLICT.getMsg();
            case 413 -> ResultCode.FILE_TOO_LARGE.getMsg();
            case 415 -> ResultCode.UNSUPPORTED_MEDIA_TYPE.getMsg();
            case 429 -> ResultCode.RATE_LIMITED.getMsg();
            case 503 -> ResultCode.SERVICE_UNAVAILABLE.getMsg();
            default -> ResultCode.INTERNAL_ERROR.getMsg();
        };
        return buildResponse(status.value(), msg, headers);
    }

    /** 数据库资源不可用或暂时性故障返回 503，不暴露 SQL 或连接信息。 */
    @ExceptionHandler({TransientDataAccessException.class, NonTransientDataAccessResourceException.class})
    public ResponseEntity<Result<Void>> handleDatabase(DataAccessException exception) {
        log.error("数据库访问失败，异常类型：{}", exception.getClass().getName());
        return ResponseEntity.status(ResultCode.SERVICE_UNAVAILABLE.getCode())
                .body(Result.error(ResultCode.SERVICE_UNAVAILABLE));
    }

    /** 未预期的内部错误返回 500，避免将异常详情发送给客户端。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpected(Exception exception) {
        log.error("请求处理失败，异常类型：{}", exception.getClass().getName());
        return ResponseEntity.status(ResultCode.INTERNAL_ERROR.getCode())
                .body(Result.error(ResultCode.INTERNAL_ERROR));
    }


    private ResponseEntity<Object> buildResponse(int code, String msg, HttpHeaders headers) {
        return new ResponseEntity<>(Result.error(code, msg), headers, HttpStatusCode.valueOf(code));
    }
}
