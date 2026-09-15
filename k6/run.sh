#!/usr/bin/env bash
# 선착순 쿠폰 발급 부하테스트 러너 (이슈 #6)
#
# 3가지 발급 전략(naive / pessimistic / redis)을 동일 조건으로 연속 실행하고,
# 전략별 요약 JSON을 모아 비교표(results/comparison.md)를 만든다.
# 발급 전략은 서버 쿼리 파라미터라서 앱 재시작 없이 전략만 바꿔 반복한다.
#
# 사용법:
#   ./run.sh                      # 기본: naive pessimistic redis, 1000 req / stock 100 / 200 VU
#   REQUESTS=2000 VUS=500 ./run.sh
#   STRATEGIES="pessimistic redis" ./run.sh
#   BASE_URL=http://localhost:8080 ./run.sh
#
# 사전 준비: 백엔드가 local 프로필(MySQL+Redis)로 떠 있어야 세 전략을 모두 측정할 수 있다.
#   docker compose up -d && (cd backend && ./gradlew bootRun)

set -euo pipefail
cd "$(dirname "$0")"

BASE_URL="${BASE_URL:-http://localhost:8080}"
REQUESTS="${REQUESTS:-1000}"
VUS="${VUS:-200}"
STOCK="${STOCK:-100}"
STRATEGIES="${STRATEGIES:-naive pessimistic redis}"
RESULTS_DIR="results"

# localhost 요청은 프록시를 타지 않도록 한다.
export NO_PROXY="localhost,127.0.0.1,${NO_PROXY:-}"
export no_proxy="$NO_PROXY"

mkdir -p "$RESULTS_DIR"

echo "▶ 대상 서버 확인: $BASE_URL"
if ! curl -fsS "$BASE_URL/api/coupons" >/dev/null 2>&1; then
  echo "✗ $BASE_URL 에 접속할 수 없습니다. 백엔드가 떠 있는지 확인하세요." >&2
  echo "  docker compose up -d && (cd backend && ./gradlew bootRun)" >&2
  exit 1
fi

for strategy in $STRATEGIES; do
  echo
  echo "════════════════════════════════════════════════════════════"
  echo "▶ [$strategy] 실행 (requests=$REQUESTS, vus=$VUS, stock=$STOCK)"
  echo "════════════════════════════════════════════════════════════"

  out="$RESULTS_DIR/$strategy.json"
  k6 run \
    -e "BASE_URL=$BASE_URL" \
    -e "STRATEGY=$strategy" \
    -e "REQUESTS=$REQUESTS" \
    -e "VUS=$VUS" \
    -e "STOCK=$STOCK" \
    -e "SUMMARY_OUT=$out" \
    issue-coupon.js

  # k6 요약과 별개로, 서버 /status 를 직접 조회해 초과 발급을 확정 기록한다.
  coupon_name="LOADTEST-쿠폰-$STOCK"
  coupon_id="$(curl -fsS "$BASE_URL/api/coupons" \
    | jq -r --arg n "$coupon_name" '.data[] | select(.name==$n) | .couponId' | head -1)"
  status="$(curl -fsS "$BASE_URL/api/coupons/$coupon_id/status")"
  total="$(echo "$status" | jq '.data.totalQuantity')"
  stock_left="$(echo "$status" | jq '.data.stock')"
  issued_rows="$(echo "$status" | jq '.data.issuedRows')"
  over=$(( issued_rows > total ? issued_rows - total : 0 ))

  # 요약 JSON에 서버 확정 수치를 병합한다.
  tmp="$(mktemp)"
  jq --argjson total "$total" \
     --argjson stockLeft "$stock_left" \
     --argjson issuedRows "$issued_rows" \
     --argjson over "$over" \
     '. + {total_quantity:$total, stock_left:$stockLeft, issued_rows:$issuedRows, over_issued:$over}' \
     "$out" > "$tmp" && mv "$tmp" "$out"

  echo "  → issuedRows=$issued_rows / total=$total / overIssued=$over / stockLeft=$stock_left"
done

# ── 비교표 생성 ────────────────────────────────────────────────────
md="$RESULTS_DIR/comparison.md"
{
  echo "# 선착순 쿠폰 발급 — 전략별 부하테스트 비교"
  echo
  echo "- 조건: 요청 ${REQUESTS}건 · 재고 ${STOCK}장 · 동시 VU ${VUS} · 대상 \`${BASE_URL}\`"
  echo "- 측정: $(date '+%Y-%m-%d %H:%M:%S %Z')"
  echo
  echo "| 전략 | 발급 성공 | 초과 발급 | 품절 거절 | 중복 거절 | 기타 실패 | RPS | 지연 avg (ms) | 지연 p95 (ms) | 지연 max (ms) |"
  echo "|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|"
  for strategy in $STRATEGIES; do
    f="$RESULTS_DIR/$strategy.json"
    [ -f "$f" ] || continue
    jq -r '"| \(.strategy) | \(.issued) | \(.over_issued) | \(.sold_out) | \(.duplicate) | \(.failed) | \(.rps) | \(.latency_avg_ms) | \(.latency_p95_ms) | \(.latency_max_ms) |"' "$f"
  done
  echo
  echo "> 초과 발급 = 실제 발급 행 수(issuedRows) − 재고(totalQuantity). 0이어야 정상."
} > "$md"

echo
echo "✔ 완료. 비교표: $md"
echo
cat "$md"
