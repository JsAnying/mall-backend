package com.zhf.mall.common;

/**
 * 项目统一响应码及默认中文提示。
 * 同一 HTTP 状态可对应不同业务提示，响应体不额外增加业务标识字段。
 */
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数有误"),
    INVALID_JSON(400, "请求体必须是有效的 JSON，字段类型须正确"),
    UNAUTHORIZED(401, "请先登录"),
    TOKEN_EXPIRED(401, "登录已过期，请重新登录"),
    INVALID_CREDENTIALS(401, "用户名或密码错误"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "请求的资源不存在"),
    GOODS_NOT_FOUND(404, "商品不存在或已删除"),
    ORDER_NOT_FOUND(404, "订单不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    NOT_ACCEPTABLE(406, "请求的响应格式不支持"),
    CONFLICT(409, "资源状态冲突，请刷新后重试"),
    USER_NOT_FOUND(404, "用户不存在"),
    PASSWORD_ERROR(401, "密码错误"),
    USERNAME_EXISTS(409, "用户名已存在，请前往登录或更换用户名"),
    GOODS_UNAVAILABLE(409, "商品已被预订，请选择其他商品"),
    INVALID_STATE(409, "当前状态不允许执行此操作"),
    IDEMPOTENCY_CONFLICT(409, "重复请求的内容不一致"),
    FILE_TOO_LARGE(413, "上传文件过大"),
    UNSUPPORTED_MEDIA_TYPE(415, "请求格式不支持，请使用 application/json"),
    UNSUPPORTED_FILE_TYPE(415, "上传文件类型不支持"),
    RATE_LIMITED(429, "请求过于频繁，请稍后重试"),
    INTERNAL_ERROR(500, "服务内部错误，请稍后重试"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用，请稍后重试");

    /** 与 HTTP 状态对应的响应码。 */
    private final int code;

    /** 可直接展示给用户的默认提示。 */
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /** 获取响应码。 */
    public int getCode() {
        return code;
    }

    /** 获取默认中文提示。 */
    public String getMsg() {
        return msg;
    }
}
