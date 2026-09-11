package com.d5wq.burger.product.dto;

import com.d5wq.burger.product.entity.OptionGroup;
import com.d5wq.burger.product.entity.Product;
import java.util.List;

/**
 * 상품 상세 조회 응답. 목록 응답({@link ProductResponse})과 달리
 * 커스터마이징을 위한 옵션 그룹(구성/재료/사이드/음료)까지 함께 내려준다.
 */
public record ProductDetailResponse(
        Long id,
        String name,
        int price,
        String imageUrl,
        String description,
        List<OptionGroupResponse> optionGroups) {

    public static ProductDetailResponse of(Product product, List<OptionGroup> groups) {
        List<OptionGroupResponse> optionGroups = groups.stream()
                .map(OptionGroupResponse::from)
                .toList();
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getImageUrl(),
                product.getDescription(),
                optionGroups);
    }
}
