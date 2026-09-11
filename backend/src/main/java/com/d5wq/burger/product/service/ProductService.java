package com.d5wq.burger.product.service;

import com.d5wq.burger.common.exception.BusinessException;
import com.d5wq.burger.common.exception.ErrorCode;
import com.d5wq.burger.product.dto.ProductDetailResponse;
import com.d5wq.burger.product.dto.ProductResponse;
import com.d5wq.burger.product.entity.OptionGroup;
import com.d5wq.burger.product.entity.Product;
import com.d5wq.burger.product.repository.OptionGroupRepository;
import com.d5wq.burger.product.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final OptionGroupRepository optionGroupRepository;

    public List<ProductResponse> getProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    /** 상세 조회는 커스터마이징을 위한 옵션 그룹(구성/재료/사이드/음료)까지 함께 내려준다. */
    public ProductDetailResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        List<OptionGroup> groups = optionGroupRepository.findByProductIdOrderByDisplayOrderAsc(id);
        return ProductDetailResponse.of(product, groups);
    }
}
