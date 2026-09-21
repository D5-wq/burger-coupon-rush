package com.d5wq.burger.coupon.repository;

import com.d5wq.burger.coupon.entity.CouponIssue;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {

    boolean existsByCouponIdAndUserId(Long couponId, Long userId);

    /** 주문에 쿠폰을 적용할 때, 그 유저가 해당 쿠폰을 실제로 발급받았는지 확인용. */
    Optional<CouponIssue> findByCouponIdAndUserId(Long couponId, Long userId);

    /**
     * 발급내역을 원자적으로 "사용됨"으로 바꾼다. (쿠폰 사용 동시성 제어)
     * {@code UPDATE ... SET used = true WHERE id = ? AND used = false} 는 DB가 행 락으로 직렬화하므로,
     * 같은 쿠폰으로 동시에 여러 주문이 들어와도 정확히 <b>한 번만</b> 1을 반환한다(나머지는 0).
     * → 이미 사용됐거나 다른 트랜잭션이 선점하면 0 → 중복 사용 차단.
     */
    @Modifying(clearAutomatically = true)
    @Query("update CouponIssue ci set ci.used = true where ci.id = :id and ci.used = false")
    int markUsedIfUnused(@Param("id") Long id);

    long countByCouponId(Long couponId);

    void deleteByCouponId(Long couponId);

    /** 내 쿠폰함: 유저가 발급받은 내역(최신순). */
    List<CouponIssue> findByUserIdOrderByIdDesc(Long userId);
}
