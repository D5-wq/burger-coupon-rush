package com.d5wq.burger.coupon.repository;

import com.d5wq.burger.coupon.entity.CouponIssue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {

    boolean existsByCouponIdAndUserId(Long couponId, Long userId);

    long countByCouponId(Long couponId);

    void deleteByCouponId(Long couponId);

    /** 내 쿠폰함: 유저가 발급받은 내역(최신순). */
    List<CouponIssue> findByUserIdOrderByIdDesc(Long userId);
}
