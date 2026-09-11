package com.d5wq.burger.common.init;

import com.d5wq.burger.coupon.entity.Coupon;
import com.d5wq.burger.coupon.repository.CouponRepository;
import com.d5wq.burger.product.entity.Product;
import com.d5wq.burger.product.repository.ProductRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 상품(버거)에 연결된 선착순 쿠폰들을 시드한다. (버거별로 할인율이 다른 예시)
 * 상품 시드({@link DataInitializer}, @Order(1)) 이후 실행되도록 @Order(2).
 */
@Slf4j
@Order(2)
@Component
@RequiredArgsConstructor
public class CouponInitializer implements CommandLineRunner {

    private final CouponRepository couponRepository;
    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (couponRepository.count() > 0) {
            log.info("[seed] coupons already exist, skip");
            return;
        }
        List<Product> products = productRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusDays(30);

        List<Coupon> coupons = new ArrayList<>();
        // 전체 주문 대상
        coupons.add(Coupon.forOrder("전체 주문 10% 할인 쿠폰", 10, 200, now, end));
        // 특정 버거 대상 (버거별 할인율 다르게)
        if (products.size() >= 1) {
            Product p = products.get(0);
            coupons.add(Coupon.forProduct(p.getName() + " 30% 할인 쿠폰", 30, p.getId(), 100, now, end));
        }
        if (products.size() >= 2) {
            Product p = products.get(1);
            coupons.add(Coupon.forProduct(p.getName() + " 25% 할인 쿠폰", 25, p.getId(), 50, now, end));
        }
        if (products.size() >= 4) {
            Product p = products.get(3);
            coupons.add(Coupon.forProduct(p.getName() + " 15% 할인 쿠폰", 15, p.getId(), 80, now, end));
        }
        couponRepository.saveAll(coupons);
        log.info("[seed] inserted {} coupons", coupons.size());
    }
}
