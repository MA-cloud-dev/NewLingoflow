package com.lingoflow.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    // 认证相关错误码
    public static final int USERNAME_EXISTS = 1001;
    public static final int EMAIL_EXISTS = 1002;
    public static final int PASSWORD_MISMATCH = 1003;
    public static final int PASSWORD_TOO_WEAK = 1004;
    public static final int INVALID_CREDENTIALS = 1010;
    public static final int ACCOUNT_DISABLED = 1011;
    public static final int INVALID_REFRESH_TOKEN = 1020;
}
