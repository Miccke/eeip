package com.eeip.common.api;

/**
 * 统一响应体。code 为字符串，成功固定 "0"。
 */
public record R<T>(String code, String message, T data) {

    public static <T> R<T> ok(T data) {
        return new R<>("0", "success", data);
    }

    public static R<Void> ok() {
        return new R<>("0", "success", null);
    }

    public static <T> R<T> fail(ErrorCode ec) {
        return new R<>(ec.code(), ec.message(), null);
    }

    public static <T> R<T> fail(ErrorCode ec, String detail) {
        return new R<>(ec.code(), detail == null ? ec.message() : detail, null);
    }
}
