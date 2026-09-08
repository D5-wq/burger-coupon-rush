package com.d5wq.burger.common.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반을 표현하는 예외. {@link ErrorCode}를 담아
 * {@link GlobalExceptionHandler}에서 일관된 응답으로 변환된다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
