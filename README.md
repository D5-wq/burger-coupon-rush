<div align="center">

# 🍔 Burger Coupon Rush

**버거 주문 앱을 만들며 백엔드의 "동시성·정합성"을 재현→해결→측정한 프로젝트**

간판 주제는 **"선착순 쿠폰 발급의 동시성 제어"** — `naive` → `비관적 락` → `Redis`로
버그를 재현하고, 제어하고, **k6 부하테스트 수치로 증명**합니다.

<sub>Spring Boot · Java 17 · JPA · Spring Security(JWT) · MySQL · Redis(Redisson) · Docker · k6</sub>

</div>

---

## 이 프로젝트는

버거 주문 서비스(회원/메뉴/주문/쿠폰)를 만들면서, **여러 요청이 같은 자원을 동시에 다툴 때 데이터 정합성이 어떻게 깨지고 어떻게 지키는지**를 끝까지 파고든 학습·실험 프로젝트입니다.

- 일반 CRUD(회원·상품·주문)는 실용적으로 빠르게.
- **깊게 파는 주제(동시성)는 "버그 재현 → 원인 분석 → 해결 → 부하테스트로 검증"**까지.
- 프론트엔드는 백엔드 흐름을 눈으로 확인하는 **데모 용도**(옵션/세트 선택 · 장바구니 · 쿠폰 적용까지 실제 동작).

> 🔗 전체 구조·ERD·동시성 상세 → **[docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md)**
> 🔗 부하테스트 방법·스크립트 → **[k6/README.md](./k6/README.md)**

---

## ⭐ 간판 1 — 선착순 쿠폰 발급 동시성 제어

재고 **100장**에 서로 다른 유저 **1,000명이 동시 요청**할 때, 발급 전략별 결과:

| 전략 | 방식 | 실제 발급(DB) | **초과 발급** | 처리량(RPS) | 지연 p95 |
|---|---|---:|---:|---:|---:|
| **naive** | 락 없음 (읽기→확인→차감) | **937장** | **+837** 🐛 | 59.8 | 341ms |
| **비관적 락** | `SELECT … FOR UPDATE` | 100장 | **0** ✅ | 117.7 | 272ms |
| **Redis** | Redisson `SADD`+`DECR` 원자연산 | 100장 | **0** ✅ | 118.4 | 316ms |

<sub>측정: k6 · 재고 100 / 요청 1,000 / 동시 VU 200 · H2 인메모리 + 로컬 Redis(단일 JVM) 기준. 절대 성능이 아니라 **정확성·상대 비교**용이며, 스크립트로 언제든 재현 가능(`k6/`).</sub>

**무슨 일이 일어났나**
- `naive`: "재고 확인 → 차감"이 원자적이지 않아, 여러 스레드가 같은 재고를 읽고 모두 통과 → **재고 100인데 937장 발급**(lost update 포함). 정확성뿐 아니라 처리량도 오히려 낮음(실패·롤백 낭비).
- `비관적 락`: 행 배타 락으로 "읽기~차감~커밋"을 **직렬화** → 초과 0. 단, 발급이 DB 락에 묶임.
- `Redis`: 경쟁 판정을 인메모리 원자연산(`SADD`=1인1장, `DECR`=재고)으로 옮겨 **경쟁 지점을 Redis가 흡수**. 초과 0 + 락 대기 없음. **여러 애플리케이션 인스턴스로 수평 확장해도 한 Redis에서 판정하므로 그대로 동작**(인메모리 락으로는 불가능한 지점).

**결론**: 단일 인스턴스면 비관적 락도 충분하지만, **다중 인스턴스 확장까지 고려하면 Redis 방식이 정확성과 확장성을 동시에** 만족. → 원인 분석(JPA 변경 감지/lost update)과 코드는 [ARCHITECTURE.md](./docs/ARCHITECTURE.md#7--핵심-선착순-쿠폰-발급의-동시성-제어).

---

## ⭐ 간판 2 — 직접 발견하고 고친 동시성 버그 (쿠폰 "사용")

발급을 막고 나서, **쿠폰 사용(주문 적용) 쪽에 같은 종류의 구멍**이 남아 있는 걸 부하 점검 중 발견했습니다.

- **재현**: 보유한 쿠폰 1장으로 **동시에 주문 10건** → 인메모리 `used` 플래그만 확인해서 **10건 전부 할인 적용**(한 쿠폰이 10번 사용).
- **해결**: 재고 차감과 동일한 패턴의 **원자적 조건부 UPDATE**로 교체
  ```sql
  UPDATE coupon_issues SET used = true WHERE id = ? AND used = false
  ```
  → DB 행 락이 사용을 직렬화, 영향 행이 0이면 이미 사용된 것 → 거절.
- **검증(수정 후)**: 동시 10건 → **1건만 할인, 9건 `COUPON_409_USED`**. 동시성 테스트로 회귀 방지.

> "발급"뿐 아니라 "사용"까지 **동시성 관점을 일관되게** 적용한 사례. ([PR #28](https://github.com/D5-wq/burger-coupon-rush/pull/28))

---

## 🍔 기능 (실제 주문 흐름)

메뉴 → **상품 상세(구성 단품/세트 · 재료 추가·제거 · 사이드 · 음료)** → 장바구니 → **쿠폰 적용** → 주문 → 내 주문.

- 주문 옵션: 서버가 상품 옵션 그룹 기준으로 **필수/최소·최대 검증**, 이름·가격을 **주문 시점 스냅샷**으로 저장, 라인가 = (기본가 + Σ옵션가) × 수량.
- 쿠폰 적용: **ORDER**(주문 합계) / **PRODUCT**(대상 상품 라인) 범위별 할인, 보유·기간·대상 검증 + 중복 사용 방지.

## 🛠 백엔드에서 다뤄본 것

| 주제 | 내용 |
|---|---|
| **동시성 제어** ⭐ | 선착순 발급(naive/락/Redis) + 쿠폰 사용 원자적 처리, k6로 수치 검증 |
| 인증/보안 | Spring Security + **JWT**(HS256), `@LoginUser` 커스텀 리졸버, STATELESS |
| 도메인 설계 | 상품↔옵션 그룹/항목, 주문 옵션 스냅샷, 쿠폰↔상품 연결, (coupon_id,user_id) 유니크로 1인 1장 |
| JPA/트랜잭션 | 영속성 컨텍스트·변경 감지, `PESSIMISTIC_WRITE`, 원자적 `@Modifying` UPDATE |
| 예외/응답 표준화 | `ErrorCode`+`BusinessException`+전역 핸들러, DB 제약 위반→409 매핑, 공통 `ApiResponse` |
| API 문서화 | **Swagger/OpenAPI**(springdoc) + JWT 인증 스킴 |
| 부하테스트 | **k6** — 전략 파라미터화, 초과 발급/RPS/지연 자동 비교표 |
| 인프라 | Docker Compose(MySQL/Redis), 프로필 분리, 인프라 없는 demo(H2) 실행 |

## 🧱 기술 스택

| 영역 | 스택 |
|---|---|
| Backend | Spring Boot 3.3, Java 17, Spring Data JPA, Spring Security + JWT |
| DB / 캐시 | MySQL 8, Redis 7(Redisson) · 테스트/데모는 H2 |
| Infra | Docker Compose |
| 부하테스트 | k6 |
| Frontend | React 18 + TypeScript + Vite + Tailwind *(데모용)* |

## ▶️ 실행

```bash
# 인프라 (MySQL + Redis)
docker compose up -d

# 백엔드 (local = MySQL + Redis, Redis 발급 전략 활성)
cd backend && ./gradlew bootRun
#   인프라 없이 빠르게 체험:  SPRING_PROFILES_ACTIVE=demo ./gradlew bootRun   # H2

# 테스트 (동시성 재현/해결 포함)
cd backend && ./gradlew test

# 부하테스트 (3전략 비교표 생성)
cd k6 && ./run.sh

# 프론트(선택, 데모)
cd frontend && npm install && npm run dev
```

- **API 명세서(Swagger)**: 백엔드 실행 후 → `http://localhost:8080/swagger-ui.html`
- 프론트 데모(mock): **https://burger-coupon-rush.vercel.app/**

## 📚 문서

- 📐 [아키텍처 & 총정리](./docs/ARCHITECTURE.md) — 구조 · ERD · 동시성 상세 · API · 개발 이력
- ⚡ [부하테스트(k6)](./k6/README.md) — 실행법 · 지표 · 결과 해석
- 🚀 [배포 가이드](./docs/DEPLOY.md)
- 🤝 [개발 컨벤션](./CONTRIBUTING.md) — Git Flow · Conventional Commits · PR 규칙

## License

[MIT](./LICENSE)
