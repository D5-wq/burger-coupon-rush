package com.d5wq.burger.coupon.entity;

import com.d5wq.burger.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저별 쿠폰 발급 내역.
 * (coupon_id, user_id) 유니크 제약으로 "1인 1장"을 DB 레벨에서 보장한다.
 * → 애플리케이션 로직이 중복을 놓치더라도, DB가 최후의 방어선이 된다.
 */
@Entity
@Getter
@Table(
        name = "coupon_issues",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_coupon_user",
                columnNames = {"coupon_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponIssue extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private CouponIssue(Long couponId, Long userId) {
        this.couponId = couponId;
        this.userId = userId;
    }

    public static CouponIssue of(Long couponId, Long userId) {
        return new CouponIssue(couponId, userId);
    }
}
