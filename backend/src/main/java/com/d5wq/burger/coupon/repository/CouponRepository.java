package com.d5wq.burger.coupon.repository;

import com.d5wq.burger.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    // Step 4에서 비관적 락 조회 메서드(findByIdForUpdate)를 여기에 추가한다.
}
