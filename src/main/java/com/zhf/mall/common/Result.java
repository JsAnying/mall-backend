package com.zhf.mall.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * 统一接口返回体，只包含响应码、提示消息和业务数据。
 *
 * @param <T> 业务数据类型
 */
@Getter
@JsonInclude(JsonInclude.Include.ALWAYS)
public class Result<T> {

    /** 响应码，与 HTTP 状态码保持一致。 */
    private final int code;

    /** 可直接展示给用户的提示消息。 */
    private final String msg;

    /** 业务数据，无返回内容或操作失败时为 null。 */
    private final T data;

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 创建携带业务数据的成功响应。
     *
     * @param data 业务数据
     * @param <T> 业务数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data);
    }

    /** 创建不携带业务数据的成功响应。 */
    public static Result<Void> success() {
        return success(null);
    }

    /** 使用枚举中的默认提示创建失败响应。 */
    public static Result<Void> error(ResultCode resultCode) {
        return error(resultCode, resultCode.getMsg());
    }

    /** 使用枚举响应码和具体提示创建失败响应。 */
    public static Result<Void> error(ResultCode resultCode, String msg) {
        return error(resultCode.getCode(), msg);
    }

    /** 适配 Spring MVC 等框架产生的 HTTP 错误状态。 */
    public static Result<Void> error(int code, String msg) {
        return new Result<>(code, msg, null);
    }
}
