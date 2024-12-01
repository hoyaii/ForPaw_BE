package com.hong.forapw.core.errors;

import com.hong.forapw.core.utils.ApiUtils;
import org.springframework.http.HttpStatus;

public class CustomException extends RuntimeException {

    private final ExceptionCode exceptionCode;
    private final String message;

    public CustomException(ExceptionCode exceptionCode) {
        this.exceptionCode = exceptionCode;
        this.message = exceptionCode.getMessage();
    }

    public CustomException(ExceptionCode exceptionCode, String message) {
        this.exceptionCode = exceptionCode;
        this.message = message;
    }

    public ApiUtils.ApiResult<?> body() {
        return ApiUtils.error(message, exceptionCode.getHttpStatus());
    }

    public HttpStatus status() {
        return exceptionCode.getHttpStatus();
    }
}
