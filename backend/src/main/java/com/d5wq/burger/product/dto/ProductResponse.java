package com.d5wq.burger.product.dto;

import com.d5wq.burger.product.entity.Product;

public record ProductResponse(
        Long id,
        String name,
        int price,
        String imageUrl,
        String description) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getImageUrl(),
                product.getDescription());
    }
}
