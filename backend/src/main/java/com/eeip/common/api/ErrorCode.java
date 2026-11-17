package com.eeip.common.api;

import org.springframework.http.HttpStatus;

/**
 * 错误码：{域}_{HTTP状态码}_{语义}
 */
public enum ErrorCode {

    OK("0", "success", HttpStatus.OK),

    BAD_REQUEST("GW_400_BAD_REQUEST", "请求参数不合法", HttpStatus.BAD_REQUEST),
    NOT_FOUND("GW_404_NOT_FOUND", "资源不存在", HttpStatus.NOT_FOUND),
    CONFLICT("GW_409_CONFLICT", "资源冲突", HttpStatus.CONFLICT),

    UNAUTHORIZED("AUTH_401_UNAUTHORIZED", "未认证或凭证无效", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("AUTH_401_TOKEN_EXPIRED", "登录已失效，请重新登录", HttpStatus.UNAUTHORIZED),
    BAD_CREDENTIALS("AUTH_401_BAD_CREDENTIALS", "用户名或密码错误", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED("AUTH_403_ACCOUNT_DISABLED", "账号已停用", HttpStatus.FORBIDDEN),
    FORBIDDEN("AUTH_403_FORBIDDEN", "无访问权限", HttpStatus.FORBIDDEN),
    USERNAME_EXISTS("SYS_409_USERNAME_EXISTS", "用户名已存在", HttpStatus.CONFLICT),
    ROLE_IN_USE("SYS_409_ROLE_IN_USE", "角色已分配给用户，无法删除", HttpStatus.CONFLICT),
    BUILTIN_ROLE("SYS_422_BUILTIN_ROLE", "内置角色不允许修改或删除", HttpStatus.UNPROCESSABLE_ENTITY),
    LAST_ADMIN("SYS_422_LAST_ADMIN", "至少保留一个可用的超级管理员", HttpStatus.UNPROCESSABLE_ENTITY),
    TENANT_NOT_FOUND("AUTH_401_TENANT_NOT_FOUND", "租户不存在", HttpStatus.UNAUTHORIZED),
    TENANT_DISABLED("AUTH_403_TENANT_DISABLED", "租户已停用", HttpStatus.FORBIDDEN),
    TENANT_EXPIRED("AUTH_403_TENANT_EXPIRED", "租户已到期", HttpStatus.FORBIDDEN),

    INTERNAL("SYS_500_INTERNAL_ERROR", "系统内部错误", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public HttpStatus status() {
        return status;
    }
}
