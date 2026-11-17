package com.gorapaduk.makingpet.common.exception;

import com.gorapaduk.makingpet.common.constants.ErrorCode;
import lombok.Getter;

/** ErrorCode 를 들고 다니는 런타임 예외. GlobalExceptionHandler 가 ApiResponse.fail 로 바꾼다. */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
