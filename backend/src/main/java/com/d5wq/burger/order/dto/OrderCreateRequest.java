package com.d5wq.burger.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderCreateRequest(
        @NotEmpty @Valid List<Line> items,
        /** 주문에 적용할 쿠폰 id(선택). 보유(발급)한 쿠폰이어야 한다. */
        Long couponId) {

    /** 쿠폰 없이 주문할 때의 편의 생성자. */
    public OrderCreateRequest(List<Line> items) {
        this(items, null);
    }

    public record Line(
            @NotNull Long productId,
            @Min(1) int quantity,
            @Valid List<OptionSelection> options) {

        /** 옵션 미선택 요청도 허용하기 위해 null 을 빈 목록으로 정규화한다. */
        public List<OptionSelection> options() {
            return options == null ? List.of() : options;
        }
    }

    /** 선택한 옵션 항목과 수량. (예: 패티 추가 항목 2개) */
    public record OptionSelection(
            @NotNull Long optionItemId,
            @Min(1) int quantity) {
    }
}
