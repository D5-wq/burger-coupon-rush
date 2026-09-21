package com.d5wq.burger.common.exception;

import com.d5wq.burger.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리. 도메인 예외/검증 실패/미처리 예외를 공통 응답으로 변환한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        log.warn("BusinessException: {} - {}", code.getCode(), e.getMessage());
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.error(code.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse(ErrorCode.INVALID_INPUT.getMessage());
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.getCode(), message));
    }

    /**
     * DB 제약 위반(예: coupon_issues 의 (coupon_id,user_id) 유니크). 극한 동시성에서
     * 애플리케이션 체크를 통과한 요청이 마지막에 DB 제약에 걸리는 경우로, 500이 아니라
     * 409(충돌)로 내려 클라이언트가 "중복/경합"임을 알 수 있게 한다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException e) {
        ErrorCode code = ErrorCode.CONFLICT;
        log.warn("DataIntegrityViolation: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.error(code.getCode(), code.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.error(code.getCode(), code.getMessage()));
    }
}
