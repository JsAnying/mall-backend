package com.zhf.mall.util;

import java.util.Locale;

/** 异常工具，用于在异常原因链中匹配底层错误信息。 */
public final class ExceptionUtil {

    private ExceptionUtil() {
    }

    /**
     * 判断异常原因链中是否存在包含指定片段的错误信息。
     *
     * @param exception 待检查的异常
     * @param fragment  错误信息片段，忽略大小写匹配
     * @return 原因链中是否存在匹配的错误信息
     */
    public static boolean hasCauseMessageContaining(Throwable exception, String fragment) {
        String expected = fragment.toLowerCase(Locale.ROOT);
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            String msg = cause.getMessage();
            if (msg != null && msg.toLowerCase(Locale.ROOT).contains(expected)) {
                return true;
            }
        }
        return false;
    }
}
