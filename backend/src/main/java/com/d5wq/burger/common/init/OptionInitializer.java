package com.d5wq.burger.common.init;

import com.d5wq.burger.product.entity.OptionGroup;
import com.d5wq.burger.product.entity.OptionItem;
import com.d5wq.burger.product.entity.Product;
import com.d5wq.burger.product.entity.SelectionType;
import com.d5wq.burger.product.repository.OptionGroupRepository;
import com.d5wq.burger.product.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 각 상품(버거)에 커스터마이징 옵션(구성/재료/사이드/음료)을 붙인다.
 * 실제 버거 앱처럼 단품/세트 선택, 재료 추가·제거(+/-), 세트 사이드 변경, 음료 R/L 을 표현한다.
 * 상품 시드({@link DataInitializer}, Order 1) 이후에 실행되며, 이미 옵션이 있으면 건너뛴다(멱등).
 */
@Slf4j
@Order(2)
@Component
@RequiredArgsConstructor
public class OptionInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final OptionGroupRepository optionGroupRepository;

    @Override
    public void run(String... args) {
        if (optionGroupRepository.count() > 0) {
            log.info("[seed] option groups already exist, skip");
            return;
        }
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            optionGroupRepository.saveAll(buildGroupsFor(product));
        }
        log.info("[seed] inserted option groups for {} products", products.size());
    }

    /** 한 상품에 붙는 표준 옵션 그룹 4종(구성/재료/사이드/음료). */
    private List<OptionGroup> buildGroupsFor(Product product) {
        return List.of(
                composition(product),
                ingredients(product),
                side(product),
                drink(product));
    }

    /** 구성: 단품/세트 중 하나 필수 선택. */
    private OptionGroup composition(Product product) {
        OptionGroup group = OptionGroup.of(product, "구성", SelectionType.SINGLE, true, 1, 1, 0);
        group.addItem(OptionItem.single("단품", 0, 0));
        group.addItem(OptionItem.single("세트 (사이드+음료)", 2500, 1));
        return group;
    }

    /** 재료 추가·제거: 기본 재료는 수량 1로 포함, +/- 로 조절하거나 추가 재료를 더한다. */
    private OptionGroup ingredients(Product product) {
        OptionGroup group = OptionGroup.of(product, "재료 추가·제거", SelectionType.MULTI, false, 0, 10, 1);
        group.addItem(OptionItem.of("양파", 0, 1, 2, 0));
        group.addItem(OptionItem.of("양상추", 0, 1, 2, 1));
        group.addItem(OptionItem.of("토마토", 0, 1, 2, 2));
        group.addItem(OptionItem.of("피클", 0, 1, 3, 3));
        group.addItem(OptionItem.of("패티 추가", 1500, 0, 2, 4));
        group.addItem(OptionItem.of("치즈 추가", 1000, 0, 2, 5));
        return group;
    }

    /** 사이드: 세트일 때 변경. 기본 감자튀김, 나머지는 추가금. */
    private OptionGroup side(Product product) {
        OptionGroup group = OptionGroup.of(product, "사이드 변경 (세트)", SelectionType.SINGLE, false, 0, 1, 2);
        group.addItem(OptionItem.single("감자튀김", 0, 0));
        group.addItem(OptionItem.single("치즈스틱", 500, 1));
        group.addItem(OptionItem.single("양념감자", 800, 2));
        group.addItem(OptionItem.single("어니언링", 800, 3));
        return group;
    }

    /** 음료: 세트일 때 선택. R 기본, L 은 +500. */
    private OptionGroup drink(Product product) {
        OptionGroup group = OptionGroup.of(product, "음료 (세트)", SelectionType.SINGLE, false, 0, 1, 3);
        group.addItem(OptionItem.single("코카콜라 R", 0, 0));
        group.addItem(OptionItem.single("코카콜라 L", 500, 1));
        group.addItem(OptionItem.single("제로콜라 R", 0, 2));
        group.addItem(OptionItem.single("제로콜라 L", 500, 3));
        group.addItem(OptionItem.single("스프라이트 R", 0, 4));
        group.addItem(OptionItem.single("스프라이트 L", 500, 5));
        return group;
    }
}
