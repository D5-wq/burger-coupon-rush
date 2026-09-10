package com.d5wq.burger.coupon.repository;

import com.d5wq.burger.coupon.entity.Coupon;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
}
