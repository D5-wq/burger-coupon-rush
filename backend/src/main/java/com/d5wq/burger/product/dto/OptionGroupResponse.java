package com.d5wq.burger.product.dto;

import com.d5wq.burger.product.entity.OptionGroup;
import com.d5wq.burger.product.entity.SelectionType;
import java.util.List;

/**
 * 상품(버거)에 붙는 옵션 묶음 응답. (예: "구성(단품/세트)", "재료 추가", "사이드", "음료")
 */
public record OptionGroupResponse(
        Long id,
        String name,
        SelectionType selectionType,
        boolean required,
        int minSelect,
        int maxSelect,
        int displayOrder,
        List<OptionItemResponse> items) {

    public static OptionGroupResponse from(OptionGroup group) {
        List<OptionItemResponse> items = group.getItems().stream()
                .map(OptionItemResponse::from)
                .toList();
        return new OptionGroupResponse(
                group.getId(),
                group.getName(),
                group.getSelectionType(),
                group.isRequired(),
                group.getMinSelect(),
                group.getMaxSelect(),
                group.getDisplayOrder(),
                items);
    }
}
