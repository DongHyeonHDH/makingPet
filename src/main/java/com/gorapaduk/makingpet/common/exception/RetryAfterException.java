package com.gorapaduk.makingpet.common.exception;

import com.gorapaduk.makingpet.common.constants.ErrorCode;
import lombok.Getter;

/** 429 응답에 Retry-After 를 붙여야 하는 예외 (쿨다운, 일일 한도). */
@Getter
public class RetryAfterException extends BusinessException {

    private final long retryAfterSeconds;

    public RetryAfterException(ErrorCode errorCode, long retryAfterSeconds, String message) {
        super(errorCode, message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
