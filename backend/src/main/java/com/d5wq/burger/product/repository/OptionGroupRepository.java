package com.d5wq.burger.product.repository;

import com.d5wq.burger.product.entity.OptionGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface OptionGroupRepository extends JpaRepository<OptionGroup, Long> {

    /** 특정 상품의 옵션 그룹들(항목까지 함께 로딩, 정렬 순서대로). */
    @EntityGraph(attributePaths = "items")
    List<OptionGroup> findByProductIdOrderByDisplayOrderAsc(Long productId);
}
