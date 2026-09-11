package com.d5wq.burger.product.dto;

import com.d5wq.burger.product.entity.OptionItem;

/**
 * 옵션 그룹 안의 선택지 응답. (예: "패티 추가 +1,500", "양파", "코카콜라 L")
 */
public record OptionItemResponse(
        Long id,
        String name,
        int extraPrice,
        int defaultQuantity,
        int maxQuantity,
        int displayOrder) {

    public static OptionItemResponse from(OptionItem item) {
        return new OptionItemResponse(
                item.getId(),
                item.getName(),
                item.getExtraPrice(),
                item.getDefaultQuantity(),
                item.getMaxQuantity(),
                item.getDisplayOrder());
    }
}
