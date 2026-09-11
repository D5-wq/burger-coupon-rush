package com.d5wq.burger.coupon.repository;

import com.d5wq.burger.coupon.entity.Coupon;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * 비관적 쓰기 락으로 쿠폰을 조회한다. (Step 4)
     * Hibernate가 {@code SELECT ... FOR UPDATE}를 실행해 해당 행에 배타적 락(X-Lock)을 건다.
     * → 락을 잡은 트랜잭션이 커밋/롤백할 때까지 다른 트랜잭션은 같은 행을 읽지 못하고 대기한다.
     * 결과적으로 "읽기 → 확인 → 차감 → 커밋"이 한 번에 하나씩만 실행되어 초과 발급이 사라진다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where c.id = :id")
    Optional<Coupon> findByIdForUpdate(@Param("id") Long id);

    /**
     * 재고를 원자적으로 1 차감한다. (Step 5 — Redis 전략에서 DB 재고 동기화용)
     * {@code UPDATE ... SET stock = stock - 1 WHERE id = ? AND stock > 0} 는 DB가 한 번에 처리하므로
     * 여러 트랜잭션이 동시에 실행해도 lost update 없이 정확히 감소한다. 실제 발급 가부는 Redis가 이미 판정하고,
     * 이 쿼리는 그 결과를 DB 재고에 반영한다. 차감된 행 수를 반환한다(0이면 이미 소진).
     */
    @Modifying(clearAutomatically = true)
    @Query("update Coupon c set c.stock = c.stock - 1 where c.id = :id and c.stock > 0")
    int decreaseStockIfAvailable(@Param("id") Long id);
}
