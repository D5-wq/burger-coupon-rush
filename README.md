<div align="center">

# 🍔 Burger Coupon Rush

**버거 주문 서비스 클론 + 선착순 쿠폰 발급의 동시성 제어 실험**

`naive` → `비관적 락(Pessimistic Lock)` → `Redis(Redisson)` 3단계로
동시성 문제를 재현하고, 제어하고, 부하테스트로 증명합니다.

</div>

---

> 🚧 **개발 진행 중** — 이 README는 Step 7에서 문제 상황 / 3가지 방식 비교 /
> 부하테스트 결과 / 결론으로 완성됩니다. 아래는 로드맵입니다.

## 왜 이 프로젝트인가

메뉴/주문 같은 일반 CRUD는 실용적으로 빠르게, **선착순 쿠폰 발급 로직만 깊이 있게**
다룹니다. "쿠폰 100장을 1000명이 동시에 받으려 할 때 어떻게 정확히 100장만
발급할 것인가"라는 전형적인 재고 경쟁(race condition) 문제를 세 가지 방식으로
구현·비교·검증하는 것이 핵심 목표입니다.

## 기술 스택

| 영역 | 스택 |
|---|---|
| Backend | Spring Boot 3.x, Java 17, Spring Data JPA, Spring Security + JWT |
| DB | MySQL 8 (로컬은 Docker Compose, AWS RDS 이전 고려 구조) |
| 캐시/락 | Redis 7 (Redisson) |
| Frontend | React + TypeScript, Tailwind CSS |
| 부하테스트 | k6 |
| 인프라 | Docker Compose |

## 로드맵

- [ ] **Step 1** — 프로젝트 세팅 + User/Product/Order 기본 CRUD ([#1](https://github.com/D5-wq/burger-coupon-rush/issues/1))
- [ ] **Step 2** — 프론트 기본 화면 (메뉴/주문/로그인) ([#2](https://github.com/D5-wq/burger-coupon-rush/issues/2))
- [ ] **Step 3** — 쿠폰 naive 버전 + 동시성 버그 재현 ([#3](https://github.com/D5-wq/burger-coupon-rush/issues/3))
- [ ] **Step 4** — 비관적 락 버전 ([#4](https://github.com/D5-wq/burger-coupon-rush/issues/4))
- [ ] **Step 5** — Redis(Redisson) 버전 ([#5](https://github.com/D5-wq/burger-coupon-rush/issues/5))
- [ ] **Step 6** — k6 부하테스트 + 3가지 비교 측정 ([#6](https://github.com/D5-wq/burger-coupon-rush/issues/6))
- [ ] **Step 7** — README 정리 ([#7](https://github.com/D5-wq/burger-coupon-rush/issues/7))

## 프로젝트 구조 (예정)

```
burger-coupon-rush/
├── backend/     # Spring Boot (Controller-Service-Repository-Entity-DTO)
├── frontend/    # React + TS + Tailwind
├── loadtest/    # k6 스크립트 & 결과
├── docs/        # ERD, 아키텍처, 측정 결과
└── docker-compose.yml
```

## 개발 컨벤션

브랜치 전략과 커밋/PR 규칙은 [CONTRIBUTING.md](./CONTRIBUTING.md) 참고.

## License

[MIT](./LICENSE)
