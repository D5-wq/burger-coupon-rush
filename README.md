<div align="center">

# 🍔 Burger Coupon Rush

**버거 주문 앱을 만들며 백엔드를 이것저것 실험하는 프로젝트**

간판 주제는 **"선착순 쿠폰 발급의 동시성 제어"** — `naive` → `비관적 락` → `Redis`로
문제를 재현하고, 제어하고, 수치로 증명합니다.

<sub>Spring Boot · Java 17 · JPA · Spring Security(JWT) · MySQL · Redis · Docker</sub>

</div>

---

## 이 프로젝트는

버거 주문 서비스(회원/메뉴/주문/쿠폰)를 만들면서 **백엔드에서 마주치는 문제들을 직접 다뤄보는 학습·실험 프로젝트**입니다.
일반 CRUD는 실용적으로 빠르게, **깊게 파는 주제(동시성 제어)는 재현→해결→검증까지** 끝까지 갑니다.
프론트엔드는 백엔드를 눈으로 확인하기 위한 **데모 용도로 가볍게** 두었습니다.

> 🔗 **전체 구조·ERD·동시성 상세는 → [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md)**

## 🛠 백엔드에서 다뤄본 것

| 주제 | 내용 |
|---|---|
| **동시성 제어** ⭐ | 선착순 쿠폰: 락 없는 초과 발급 재현 → 비관적 락 → (예정)Redis. 결과를 수치로 비교 |
| 인증/보안 | Spring Security + **JWT**(HS256), `@LoginUser` 커스텀 리졸버, STATELESS |
| 계층형 아키텍처 | Controller-Service-Repository-Entity-DTO, 도메인별 패키징 |
| 도메인 설계 | 쿠폰↔상품 연결(버거별 할인율), 유니크 제약으로 1인 1장 보장 |
| JPA/트랜잭션 | 영속성 컨텍스트·변경 감지·`PESSIMISTIC_WRITE` 락 이해와 적용 |
| 예외/응답 표준화 | `ErrorCode`+`BusinessException`+전역 핸들러, 공통 `ApiResponse` |
| 인프라 | Docker Compose(MySQL/Redis), 프로필 분리, 인프라 없는 demo(H2) 실행 |
| (예정) | Redis 분산락 · k6 부하테스트 · CI/CD(GitHub Actions) |

## ⭐ 간판: 선착순 쿠폰 동시성 제어

재고 **100** 쿠폰에 서로 다른 유저 **1000명 동시 요청** (통합 테스트):

| 방식 | 실제 발급 | 초과 발급 |
|---|---|---|
| **naive** (락 없음) | ~968장 | **+868** 🐛 |
| **비관적 락** (`SELECT … FOR UPDATE`) | **100장** | **0** ✅ |
| **Redis** (Redisson) | — | *Step 5 예정* |

> "재고 확인 → 차감"이 원자적이지 않아 여러 스레드가 같은 재고를 읽고 모두 통과 → 초과 발급.
> 원인 분석(JPA 변경 감지/lost update)과 해결 과정은 [ARCHITECTURE.md](./docs/ARCHITECTURE.md#7--핵심-선착순-쿠폰-발급의-동시성-제어) 참고.

## 🧱 기술 스택

| 영역 | 스택 |
|---|---|
| Backend | Spring Boot 3.3, Java 17, Spring Data JPA, Spring Security + JWT |
| DB / 캐시 | MySQL 8, Redis 7(Redisson) · 테스트/데모는 H2 |
| Infra | Docker Compose |
| 부하테스트 | k6 *(예정)* |
| Frontend | React 18 + TypeScript + Vite + Tailwind *(데모용, 가볍게)* |

## ▶️ 실행

```bash
# 인프라 (MySQL + Redis)
docker compose up -d

# 백엔드
cd backend && ./gradlew bootRun
#   인프라 없이 빠르게 체험:  SPRING_PROFILES_ACTIVE=demo ./gradlew bootRun   # H2

# 테스트 (동시성 재현/해결 포함)
cd backend && ./gradlew test

# 프론트(선택, 데모)
cd frontend && npm install && npm run dev
```

프론트 데모(mock): **https://burger-coupon-rush.vercel.app/**

## 📚 문서

- 📐 [아키텍처 & 총정리](./docs/ARCHITECTURE.md) — 구조 · ERD · 동시성 상세 · API · 개발 이력
- 🚀 [배포 가이드](./docs/DEPLOY.md)
- 🤝 [개발 컨벤션](./CONTRIBUTING.md) — Git Flow · Conventional Commits · PR 규칙

## License

[MIT](./LICENSE)
