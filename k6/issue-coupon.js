// 선착순 쿠폰 발급 부하테스트 (Step 6, 이슈 #6)
//
// 동일 조건(쿠폰 재고 STOCK장, 총 REQUESTS건의 동시 발급 요청)으로
// 3가지 발급 전략(naive / pessimistic / redis)을 각각 측정한다.
// 전략은 서버의 쿼리 파라미터(?strategy=)로 런타임에 고를 수 있으므로,
// 앱을 재시작하지 않고 STRATEGY 환경변수만 바꿔 반복 실행한다.
//
// "1인 1장" 제약(coupon_issues 유니크) 때문에, 재고 경쟁이 유일한 병목이 되도록
// 요청마다 서로 다른 유저 토큰을 사용한다. setup()에서 REQUESTS명을 미리 만든다.
//
// 실행 예:
//   k6 run -e STRATEGY=naive      issue-coupon.js
//   k6 run -e STRATEGY=pessimistic issue-coupon.js
//   k6 run -e STRATEGY=redis      issue-coupon.js
// (보통은 run.sh 로 세 전략을 연속 실행하고 결과를 표로 모은다.)

import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import exec from 'k6/execution';

// ── 설정 (환경변수로 조정) ──────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const STRATEGY = __ENV.STRATEGY || 'naive'; // naive | pessimistic | redis
const REQUESTS = parseInt(__ENV.REQUESTS || '1000', 10); // 총 발급 시도 수
const VUS = parseInt(__ENV.VUS || '200', 10); // 동시 가상 유저 수
const STOCK = parseInt(__ENV.STOCK || '100', 10); // 쿠폰 재고(=totalQuantity)
const USER_PREFIX = __ENV.USER_PREFIX || 'loadtest';
const PASSWORD = __ENV.PASSWORD || 'loadtest1234';
// 재고가 다르면 별도 쿠폰으로 취급(既존 쿠폰의 totalQuantity 는 생성 후 고정이므로).
const COUPON_NAME = __ENV.COUPON_NAME || `LOADTEST-쿠폰-${STOCK}`;
// 커스텀 요약 JSON 출력 경로(run.sh 가 지정). 없으면 파일은 안 쓴다.
const SUMMARY_OUT = __ENV.SUMMARY_OUT || '';

// ── 결과 분류 카운터 ────────────────────────────────────────────────
const issued = new Counter('coupon_issued'); // 발급 성공(200)
const soldOut = new Counter('coupon_sold_out'); // 재고 소진(409 SOLD_OUT)
const duplicate = new Counter('coupon_duplicate'); // 이미 발급(409 DUP)
const failed = new Counter('coupon_failed'); // 그 밖의 실패

export const options = {
  scenarios: {
    issue: {
      executor: 'shared-iterations',
      vus: VUS,
      iterations: REQUESTS,
      maxDuration: '3m',
    },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(95)', 'p(99)', 'max'],
  // 응답 시간 자체가 측정 대상이라 임계값으로 실행을 실패시키지는 않는다.
  thresholds: {},
};

const JSON_HEADERS = { 'Content-Type': 'application/json' };

// ── setup: 유저/쿠폰 준비 + 재고 리셋 ───────────────────────────────
export function setup() {
  console.log(
    `[setup] strategy=${STRATEGY} requests=${REQUESTS} vus=${VUS} stock=${STOCK} base=${BASE_URL}`
  );

  // 쿠폰 생성/리셋은 인증이 필요하므로 유저를 먼저 만들고 첫 토큰을 관리자 용도로 쓴다.
  const tokens = ensureUsers(REQUESTS);
  if (tokens.length === 0) {
    throw new Error('유저 토큰을 하나도 확보하지 못했습니다.');
  }
  const adminToken = tokens[0];
  const couponId = ensureCoupon(adminToken);
  resetCoupon(couponId, adminToken);

  if (tokens.length < REQUESTS) {
    console.warn(
      `[setup] 토큰 ${tokens.length}/${REQUESTS}개만 확보됨 — 일부 요청은 유저를 재사용한다.`
    );
  }
  console.log(`[setup] ready: couponId=${couponId}, tokens=${tokens.length}`);
  return { couponId, tokens };
}

// 부하 본체: 요청마다 고유 유저 토큰으로 1회 발급 시도
export default function (data) {
  const idx = exec.scenario.iterationInTest % data.tokens.length;
  const token = data.tokens[idx];

  const res = http.post(
    `${BASE_URL}/api/coupons/${data.couponId}/issue?strategy=${STRATEGY}`,
    null,
    { headers: { Authorization: `Bearer ${token}` }, tags: { name: 'issue' } }
  );

  const code = errorCode(res);
  if (res.status === 200) {
    issued.add(1);
  } else if (res.status === 409 && code === 'COUPON_409_SOLD_OUT') {
    soldOut.add(1);
  } else if (res.status === 409 && code === 'COUPON_409_DUP') {
    duplicate.add(1);
  } else {
    failed.add(1);
  }

  // 성공 또는 "정상적인 거절"(품절/중복)이면 기대한 응답으로 본다.
  check(res, {
    'status is 200 or 409': (r) => r.status === 200 || r.status === 409,
  });
}

// ── teardown: 최종 현황 로그 (초과 발급 확인) ───────────────────────
export function teardown(data) {
  const status = getStatus(data.couponId);
  if (!status) {
    console.warn('[teardown] status 조회 실패');
    return;
  }
  const overIssued = Math.max(0, status.issuedRows - status.totalQuantity);
  console.log(
    `[teardown] total=${status.totalQuantity} stock=${status.stock} ` +
      `issuedRows=${status.issuedRows} overIssued=${overIssued}`
  );
}

// ── 요약 출력: 콘솔 + (선택) 커스텀 JSON 파일 ───────────────────────
export function handleSummary(data) {
  const m = data.metrics;
  const compact = {
    strategy: STRATEGY,
    requests: REQUESTS,
    vus: VUS,
    stock: STOCK,
    http_reqs: num(m.http_reqs, 'count'),
    rps: round(num(m.http_reqs, 'rate'), 1),
    latency_avg_ms: round(num(m.http_req_duration, 'avg'), 1),
    latency_p95_ms: round(num(m.http_req_duration, 'p(95)'), 1),
    latency_p99_ms: round(num(m.http_req_duration, 'p(99)'), 1),
    latency_max_ms: round(num(m.http_req_duration, 'max'), 1),
    issued: num(m.coupon_issued, 'count'),
    sold_out: num(m.coupon_sold_out, 'count'),
    duplicate: num(m.coupon_duplicate, 'count'),
    failed: num(m.coupon_failed, 'count'),
  };

  const out = {};
  out.stdout = renderText(compact);
  if (SUMMARY_OUT) {
    out[SUMMARY_OUT] = JSON.stringify(compact, null, 2);
  }
  return out;
}

// ── 헬퍼 ────────────────────────────────────────────────────────────
function ensureCoupon(token) {
  const list = http.get(`${BASE_URL}/api/coupons`);
  const body = safeJson(list);
  const found = (body && body.data ? body.data : []).find(
    (c) => c.name === COUPON_NAME
  );
  if (found) {
    return found.couponId || found.id;
  }
  const res = http.post(
    `${BASE_URL}/api/coupons`,
    JSON.stringify({
      name: COUPON_NAME,
      discountRate: 10,
      applyScope: 'ORDER',
      totalQuantity: STOCK,
    }),
    { headers: authJson(token) }
  );
  const created = safeJson(res);
  if (res.status !== 201 || !created || !created.data) {
    throw new Error(`쿠폰 생성 실패: status=${res.status} body=${res.body}`);
  }
  return created.data.couponId;
}

function ensureUsers(count) {
  const tokens = [];
  const CHUNK = 50;
  for (let start = 0; start < count; start += CHUNK) {
    const end = Math.min(start + CHUNK, count);

    // 1) 회원가입(이미 있으면 409 → 무시). 배치로 병렬 처리.
    const signups = [];
    for (let i = start; i < end; i++) {
      signups.push([
        'POST',
        `${BASE_URL}/api/auth/signup`,
        JSON.stringify({
          email: email(i),
          password: PASSWORD,
          name: `load-${i}`,
        }),
        { headers: JSON_HEADERS },
      ]);
    }
    http.batch(signups);

    // 2) 로그인 → 토큰. 배치로 병렬 처리.
    const logins = [];
    for (let i = start; i < end; i++) {
      logins.push([
        'POST',
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({ email: email(i), password: PASSWORD }),
        { headers: JSON_HEADERS },
      ]);
    }
    const res = http.batch(logins);
    for (const r of res) {
      const body = safeJson(r);
      if (body && body.data && body.data.accessToken) {
        tokens.push(body.data.accessToken);
      }
    }
  }
  return tokens;
}

function resetCoupon(couponId, token) {
  const res = http.post(`${BASE_URL}/api/coupons/${couponId}/reset`, null, {
    headers: authJson(token),
  });
  if (res.status !== 200) {
    throw new Error(`쿠폰 리셋 실패: status=${res.status} body=${res.body}`);
  }
}

function getStatus(couponId) {
  const res = http.get(`${BASE_URL}/api/coupons/${couponId}/status`);
  const body = safeJson(res);
  return body && body.data ? body.data : null;
}

function email(i) {
  return `${USER_PREFIX}+${i}@burger.test`;
}

function authJson(token) {
  return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
}

function errorCode(res) {
  const body = safeJson(res);
  return body && body.error ? body.error.code : null;
}

function safeJson(res) {
  try {
    return res.json();
  } catch (e) {
    return null;
  }
}

function num(metric, key) {
  return metric && metric.values && metric.values[key] != null
    ? metric.values[key]
    : 0;
}

function round(v, digits) {
  const p = Math.pow(10, digits);
  return Math.round(v * p) / p;
}

function renderText(c) {
  const lines = [
    '',
    `── 결과 요약 [strategy=${c.strategy}] ──────────────────────────`,
    `요청/재고        : ${c.requests} req  /  stock ${c.stock}  (VU ${c.vus})`,
    `처리량(RPS)      : ${c.rps}`,
    `지연 avg/p95/max : ${c.latency_avg_ms} / ${c.latency_p95_ms} / ${c.latency_max_ms} ms`,
    `발급 성공        : ${c.issued}`,
    `품절 거절        : ${c.sold_out}`,
    `중복 거절        : ${c.duplicate}`,
    `기타 실패        : ${c.failed}`,
    '────────────────────────────────────────────────────',
    '',
  ];
  return lines.join('\n');
}
