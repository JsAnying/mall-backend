package com.zhf.mall.exception;

import com.zhf.mall.common.ResultCode;
import lombok.Getter;

/**
 * 可向用户展示的业务异常，由全局异常处理器转换为统一返回体。
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务错误对应的响应码与默认提示。 */
    private final ResultCode resultCode;

    /** 使用枚举的默认提示。 */
    public BusinessException(ResultCode resultCode) {
        this(resultCode, resultCode.getMsg());
    }

    /** 在保留统一响应码的同时，提供具体业务提示。 */
    public BusinessException(ResultCode resultCode, String msg) {
        super(msg);
        this.resultCode = resultCode;
    }
}
