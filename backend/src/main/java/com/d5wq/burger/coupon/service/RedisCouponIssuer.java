package com.d5wq.burger.coupon.service;

import com.d5wq.burger.common.exception.BusinessException;
import com.d5wq.burger.common.exception.ErrorCode;
import com.d5wq.burger.coupon.entity.Coupon;
import com.d5wq.burger.coupon.entity.CouponIssue;
import com.d5wq.burger.coupon.repository.CouponIssueRepository;
import com.d5wq.burger.coupon.repository.CouponRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Step 5: Redis(Redisson) 원자 연산 기반 발급.
 *
 * <p>비관적 락(Step 4)은 정확하지만 DB 행 락으로 발급을 <b>한 건씩 직렬화</b>해 처리량이 DB에 묶인다.
 * 여기서는 재고 판정을 <b>인메모리 Redis의 원자 연산</b>으로 옮긴다.
 * <ul>
 *   <li>1인 1장: {@code SADD}(집합에 추가)는 원자적 — 이미 있으면 실패로 중복을 막는다.</li>
 *   <li>재고 차감: {@code DECR}(카운터 감소)는 원자적 — 수천 요청이 동시에 들어와도 정확히 감소한다.</li>
 * </ul>
 * DB는 발급 기록(누가 받았는지)과 재고 동기화만 담당하고, "경쟁 지점"은 Redis가 흡수한다.
 * 여러 애플리케이션 인스턴스로 늘려도 Redis 한 곳에서 판정하므로 그대로 동작한다.
 *
 * <p>인프라가 필요하므로 {@code coupon.redis-enabled=true} 일 때만 빈으로 등록된다
 * (demo/test 기본 프로필에서는 비활성).
 */
@Component
@ConditionalOnProperty(prefix = "coupon", name = "redis-enabled", havingValue = "true")
@RequiredArgsConstructor
public class RedisCouponIssuer implements CouponIssuer {

    private final RedissonClient redisson;
    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;

    @Override
    @Transactional
    public void issue(Long couponId, Long userId) {
        // 발급 기간/존재 확인은 DB 기준(가벼운 SELECT, 락 없음).
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
        if (!coupon.isOpen(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED);
        }

        RSet<Long> issued = redisson.getSet(issuedKey(couponId));
        RAtomicLong stock = redisson.getAtomicLong(stockKey(couponId));

        // 재고 카운터가 아직 없으면(시드 전) DB 재고로 1회 초기화한다.
        // 키가 없으면 0으로 취급되므로 compareAndSet(0, 재고)가 원자적 "없을 때만 설정" 역할을 한다.
        // 여러 스레드가 동시에 들어와도 최초 1개만 성공하고 나머지는 무시된다.
        if (!stock.isExists()) {
            stock.compareAndSet(0, coupon.getStock());
        }

        // ① 1인 1장 — SADD 는 원자적. 이미 발급받았으면 add 가 false.
        if (!issued.add(userId)) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        // ② 재고 차감 — DECR 은 원자적. 감소 결과가 음수면 재고를 넘은 것이므로 되돌린다.
        long remaining = stock.decrementAndGet();
        if (remaining < 0) {
            stock.incrementAndGet();   // 재고 복구
            issued.remove(userId);     // 발급 표시 취소
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
        }

        // ③ 여기까지 통과한 요청만 DB에 반영한다(발급 기록 + 재고 동기화).
        try {
            couponRepository.decreaseStockIfAvailable(couponId);
            couponIssueRepository.save(CouponIssue.of(couponId, userId));
        } catch (RuntimeException e) {
            // DB 반영 실패 시 Redis 상태를 보상해 재고가 새는 것을 막는다.
            stock.incrementAndGet();
            issued.remove(userId);
            throw e;
        }
    }

    /**
     * 재고 카운터/발급 집합을 초기화한다. (부하테스트·버그 재현 반복용)
     * {@link CouponIssueService#reset(Long)} 에서 DB 재고 리셋과 함께 호출된다.
     */
    public void resetCache(Long couponId, int stock) {
        redisson.getSet(issuedKey(couponId)).delete();
        redisson.getAtomicLong(stockKey(couponId)).set(stock);
    }

    @Override
    public IssueStrategy strategy() {
        return IssueStrategy.REDIS;
    }

    private String stockKey(Long couponId) {
        return "coupon:" + couponId + ":stock";
    }

    private String issuedKey(Long couponId) {
        return "coupon:" + couponId + ":issued";
    }
}
