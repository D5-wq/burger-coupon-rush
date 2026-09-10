package com.d5wq.burger.coupon.entity;

import com.d5wq.burger.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "coupons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** 할인율(%). 예: 20 → 20% 할인. */
    @Column(nullable = false)
    private int discountRate;

    /** 총 발급 수량(고정). */
    @Column(nullable = false)
    private int totalQuantity;

    /** 남은 재고. 발급될 때마다 1씩 줄어든다. (Step 3에서 이 값이 음수로 터지는 걸 재현) */
    @Column(nullable = false)
    private int stock;

    /** 발급 가능 기간. */
    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Builder
    private Coupon(String name, int discountRate, int totalQuantity,
            LocalDateTime startAt, LocalDateTime endAt) {
        this.name = name;
        this.discountRate = discountRate;
        this.totalQuantity = totalQuantity;
        this.stock = totalQuantity; // 시작 재고 = 총 수량
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public static Coupon create(String name, int discountRate, int totalQuantity,
            LocalDateTime startAt, LocalDateTime endAt) {
        return Coupon.builder()
                .name(name)
                .discountRate(discountRate)
                .totalQuantity(totalQuantity)
                .startAt(startAt)
                .endAt(endAt)
                .build();
    }

    /** 지금이 발급 가능한 기간인지. */
    public boolean isOpen(LocalDateTime now) {
        return !now.isBefore(startAt) && !now.isAfter(endAt);
    }

    public boolean hasStock() {
        return stock > 0;
    }

    /**
     * 재고를 1 차감한다. (가드 없음 — 재고 확인은 호출자 책임)
     * Step 3 naive 버전에서 "확인 후 차감"이 원자적이지 않아 음수가 되는 지점.
     */
    public void decreaseStock() {
        this.stock -= 1;
    }

    /** 이미 발급된 수량 = 총 수량 - 남은 재고. */
    public int issuedQuantity() {
        return totalQuantity - stock;
    }

    /** 재고를 총 수량으로 되돌린다. (부하테스트/버그 재현 반복용) */
    public void resetStock() {
        this.stock = totalQuantity;
    }
}
