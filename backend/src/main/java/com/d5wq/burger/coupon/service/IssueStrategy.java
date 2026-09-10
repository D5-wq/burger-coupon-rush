package com.d5wq.burger.coupon.service;

/**
 * 쿠폰 발급 동시성 제어 전략. Step 3~5에서 하나씩 구현해 비교한다.
 */
public enum IssueStrategy {
    NAIVE,        // Step 3: 락 없음 (버그 재현용)
    PESSIMISTIC,  // Step 4: DB 비관적 락
    REDIS         // Step 5: Redis(Redisson) 분산락/원자연산
}
