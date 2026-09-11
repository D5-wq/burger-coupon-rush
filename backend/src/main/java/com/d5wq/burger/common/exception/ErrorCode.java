package com.d5wq.burger.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 도메인 전반에서 사용하는 에러 코드.
 * code 는 프론트가 분기하기 좋은 안정적인 문자열, status 는 HTTP 상태.
 */
@Getter
public enum ErrorCode {

    // common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 오류가 발생했습니다."),

    // auth / user
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_409", "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_401", "이메일 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401_2", "인증이 필요합니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404", "사용자를 찾을 수 없습니다."),

    // product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_404", "상품을 찾을 수 없습니다."),

    // order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_404", "주문을 찾을 수 없습니다."),
    EMPTY_ORDER(HttpStatus.BAD_REQUEST, "ORDER_400", "주문 항목이 비어 있습니다."),

    // order option
    INVALID_OPTION(HttpStatus.BAD_REQUEST, "OPTION_400", "해당 상품에서 선택할 수 없는 옵션입니다."),
    OPTION_QUANTITY_EXCEEDED(HttpStatus.BAD_REQUEST, "OPTION_400_QTY", "옵션 수량이 허용 범위를 벗어났습니다."),
    REQUIRED_OPTION_MISSING(HttpStatus.BAD_REQUEST, "OPTION_400_REQUIRED", "필수 옵션을 선택해야 합니다."),
    OPTION_SELECTION_INVALID(HttpStatus.BAD_REQUEST, "OPTION_400_COUNT", "옵션 선택 개수가 올바르지 않습니다."),

    // coupon (Step 3~5)
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "COUPON_404", "쿠폰을 찾을 수 없습니다."),
    COUPON_SOLD_OUT(HttpStatus.CONFLICT, "COUPON_409_SOLD_OUT", "쿠폰이 모두 소진되었습니다."),
    COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, "COUPON_409_DUP", "이미 발급받은 쿠폰입니다."),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "COUPON_400_EXPIRED", "쿠폰 발급 기간이 아닙니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
