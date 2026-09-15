# k6 부하테스트 — 선착순 쿠폰 발급 (이슈 #6)

선착순 쿠폰 발급의 **동시성 제어 3가지 방식**을 동일 조건으로 부하테스트하고
결과를 한 표로 비교한다.

| 전략 | 방식 | 기대 결과 |
|---|---|---|
| `naive` | 락 없음 (읽기→확인→차감) | **초과 발급 발생** (race / lost update) |
| `pessimistic` | DB 비관적 락 `SELECT … FOR UPDATE` | 초과 발급 0, 발급이 직렬화되어 DB에 묶임 |
| `redis` | Redis 원자연산 `SADD`+`DECR` | 초과 발급 0, 경쟁 지점을 Redis가 흡수 |

측정 지표: 발급 성공 수, **초과 발급 수**, 품절/중복 거절 수, 처리량(RPS),
응답 지연(avg / p95 / max).

---

## 무엇을 측정하나

- **정확성**: 재고 `STOCK`장에 대해 `REQUESTS`건이 동시에 몰릴 때 실제 발급된
  행 수(`issuedRows`)가 재고를 넘는가? → `over_issued = issuedRows − totalQuantity`.
- **성능**: 같은 부하에서 전략별 처리량과 지연.

"1인 1장"(`coupon_issues` 유니크) 제약 때문에, **재고 경쟁만 순수하게 측정**되도록
요청마다 서로 다른 유저 토큰을 쓴다. `setup()`에서 `REQUESTS`명을 미리 만든다.

발급 전략은 서버의 쿼리 파라미터(`POST /api/coupons/{id}/issue?strategy=`)라서
**앱을 재시작하지 않고** 전략만 바꿔 반복 실행한다.

---

## 준비물

- [k6](https://k6.io/docs/get-started/installation/)
- 세 전략을 모두 측정하려면 백엔드가 **`local` 프로필(MySQL + Redis)**로 떠 있어야 한다
  (Redis 전략은 `coupon.redis-enabled=true`인 `local`에서만 활성).

```bash
# 1) 인프라 (MySQL + Redis)
docker compose up -d

# 2) 백엔드 (local 프로필 = MySQL + Redis)
cd backend && ./gradlew bootRun
```

> `demo` 프로필(H2, Redis 제외)로는 `naive`/`pessimistic`만 측정할 수 있다.

---

## 실행

```bash
cd k6

# 세 전략 연속 실행 + 비교표 생성 (기본: 1000 req / stock 100 / 200 VU)
./run.sh

# 조건 바꾸기
REQUESTS=2000 VUS=500 STOCK=100 ./run.sh
STRATEGIES="pessimistic redis" ./run.sh
BASE_URL=http://localhost:8080 ./run.sh
```

단일 전략만 직접 돌리기:

```bash
k6 run -e STRATEGY=naive -e REQUESTS=1000 -e VUS=200 -e STOCK=100 issue-coupon.js
```

### 환경변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `BASE_URL` | `http://localhost:8080` | 대상 서버 |
| `STRATEGY` | `naive` | `naive` \| `pessimistic` \| `redis` (단일 실행 시) |
| `STRATEGIES` | `naive pessimistic redis` | run.sh 가 순회할 전략들 |
| `REQUESTS` | `1000` | 총 발급 시도 수(=미리 만드는 유저 수) |
| `VUS` | `200` | 동시 가상 유저 수 |
| `STOCK` | `100` | 쿠폰 재고(=totalQuantity) |

---

## 결과물

- `results/<strategy>.json` — 전략별 요약(요청/성공/초과발급/RPS/지연…)
- `results/comparison.md` — 전략 비교표 (run.sh 가 생성)

`run.sh`는 k6 요약과 별개로 서버 `/status`를 직접 조회해 **초과 발급을 확정 기록**한다.

---

## 동작 방식 (스크립트 내부)

1. **setup**
   - 쿠폰 확보: `LOADTEST-쿠폰-<STOCK>` 이 없으면 `POST /api/coupons`로 생성
     (재고가 다르면 별도 쿠폰으로 취급 — `totalQuantity`는 생성 후 고정이므로).
   - 유저 확보: `loadtest+<i>@burger.test` 로 회원가입(이미 있으면 409 무시) 후 로그인해 토큰 수집.
   - 재고 리셋: `POST /api/coupons/{id}/reset` (발급 내역 삭제 + 재고 원복, Redis 카운터도 초기화).
2. **부하**: `shared-iterations` 로 `REQUESTS`건을 `VUS`명이 나눠 실행.
   각 요청은 `iterationInTest` 인덱스로 **고유 토큰**을 골라 1회 발급 시도.
   응답을 성공/품절/중복/기타로 분류.
3. **teardown**: `/status`로 최종 재고·발급행수·초과발급을 로그.

---

## 예시 출력

> 아래는 **H2 인메모리 + 로컬 Redis, 단일 JVM** 환경의 스모크 실행 결과다.
> 절대 성능(RPS·지연)은 MySQL 기반 정식 벤치와 다르지만, **정확성 비교(초과 발급)는 동일하게 성립**한다.
> 정식 수치는 `docker compose`(MySQL+Redis) + `local` 프로필에서 `./run.sh`로 측정한다.

조건: 요청 500건 · 재고 50장 · 동시 VU 200

| 전략 | 발급 성공 | 초과 발급 | 품절 거절 | 중복 거절 | 기타 실패 | RPS | 지연 avg (ms) | 지연 p95 (ms) | 지연 max (ms) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| naive | 316 | **266** | 184 | 0 | 0 | 54.8 | 201.5 | 510.0 | 833.4 |
| pessimistic | 50 | 0 | 450 | 0 | 0 | 108.7 | 137.4 | 446.8 | 710.4 |
| redis | 50 | 0 | 450 | 0 | 0 | 110.3 | 128.4 | 435.0 | 655.9 |

- **naive**: 재고 50장인데 **316장이 발급**됐다(초과 266). 락 없이 "확인→차감"이 갈라져 여러 요청이 같은 재고를 보고 통과한 전형적인 race/lost update.
- **pessimistic / redis**: 정확히 50장만 발급, 초과 0. 나머지는 품절로 거절.
