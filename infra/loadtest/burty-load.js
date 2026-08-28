// BURTY 부하 시나리오 (k6)
//
// 실행:
//   k6 run -e BASE_URL=http://localhost:8080 -e TOKEN=<JWT> infra/loadtest/burty-load.js
//
// 목적은 "몇 RPS 를 버티나" 가 아니다. 현재 설정값이 합리적 기본값일 뿐 실측이 아니라서,
// **무엇이 먼저 무너지는지**를 찾는 것이 목적이다. 커넥션 풀(20), HTTP 클라이언트,
// 스케줄러 풀(8) 중 어디가 병목인지 확인하고 그 값을 조정한다.
//
// 주의: 이체는 실제로 돈을 움직인다. stub-mode 가 켜진 환경에서만 돌릴 것.
// 운영을 대상으로 실행하지 말 것.

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN;

if (!TOKEN) {
  throw new Error('TOKEN 환경변수가 필요합니다. POST /api/v1/auth/token 으로 발급하세요.');
}

const headers = {
  Authorization: `Bearer ${TOKEN}`,
  'Content-Type': 'application/json',
};

// 실패율과 지연을 경로별로 나눠 본다. 전체 평균만 보면 느린 경로가 묻힌다.
const transferFailures = new Rate('burty_transfer_failures');
const readLatency = new Trend('burty_read_latency', true);
const writeLatency = new Trend('burty_write_latency', true);

export const options = {
  scenarios: {
    // 대부분의 트래픽은 조회다. 실제 사용 패턴에 맞춘다.
    reads: {
      executor: 'ramping-vus',
      exec: 'readScenario',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 30 },
        { duration: '3m', target: 30 },
        { duration: '1m', target: 80 },   // 부하 상승 — 여기서 무너지는 지점을 본다
        { duration: '2m', target: 80 },
        { duration: '1m', target: 0 },
      ],
    },
    // 이체는 빈도가 낮지만 가장 비싼 경로다 (은행 호출 + 트랜잭션 + 아웃박스).
    transfers: {
      executor: 'constant-arrival-rate',
      exec: 'transferScenario',
      rate: 5,
      timeUnit: '1s',
      duration: '8m',
      preAllocatedVUs: 20,
      maxVUs: 60,
    },
  },
  thresholds: {
    // 조회는 사용자가 기다리는 화면이다.
    'burty_read_latency': ['p(95)<500'],
    // 이체는 은행 왕복이 포함되므로 느슨하게 잡되, 무한정은 아니다.
    'burty_write_latency': ['p(95)<3000'],
    'burty_transfer_failures': ['rate<0.01'],
    'http_req_failed': ['rate<0.02'],
  },
};

export function readScenario() {
  group('조회', () => {
    const res = http.get(`${BASE_URL}/api/v1/transactions?page=0&size=50`, { headers });
    readLatency.add(res.timings.duration);
    check(res, { '거래내역 200': (r) => r.status === 200 });

    const budgets = http.get(`${BASE_URL}/api/v1/budgets`, { headers });
    readLatency.add(budgets.timings.duration);
    check(budgets, { '예산 200': (r) => r.status === 200 });

    const limits = http.get(`${BASE_URL}/api/v1/settings/limits`, { headers });
    readLatency.add(limits.timings.duration);
    check(limits, { '한도 200': (r) => r.status === 200 });
  });
  sleep(Math.random() * 2 + 1);
}

export function transferScenario() {
  group('이체', () => {
    // 멱등키는 매번 달라야 한다. 같은 키를 재사용하면 두 번째부터는 재요청으로 처리되어
    // 실제 이체 경로를 타지 않고, 부하 시험의 의미가 없어진다.
    const idempotencyKey = `load-${__VU}-${__ITER}-${Date.now()}`;
    const payload = JSON.stringify({
      fromAccount: '1234567890',
      toAccount: '9876543210',
      amount: 1000,
      description: 'load test',
      assertionToken: 'load-test-assertion',
      idempotencyKey,
    });

    const res = http.post(`${BASE_URL}/api/v1/transfers`, payload, { headers });
    writeLatency.add(res.timings.duration);

    // 한도 초과(9002)와 승인 필요(9005)는 정상 응답이다. 실패로 세지 않는다.
    const expected = res.status === 200 || res.status === 400 || res.status === 403;
    transferFailures.add(!expected);
    check(res, { '이체 응답 정상': () => expected });
  });
}

export function handleSummary(data) {
  return {
    stdout: `
=== BURTY 부하 시험 결과 ===

조회 p95      : ${data.metrics.burty_read_latency?.values['p(95)']?.toFixed(0) ?? '-'} ms
이체 p95      : ${data.metrics.burty_write_latency?.values['p(95)']?.toFixed(0) ?? '-'} ms
이체 실패율   : ${((data.metrics.burty_transfer_failures?.values.rate ?? 0) * 100).toFixed(2)} %
전체 HTTP 실패: ${((data.metrics.http_req_failed?.values.rate ?? 0) * 100).toFixed(2)} %

시험 중 아래 지표를 함께 볼 것 (Grafana):
  hikaricp_connections_pending      커넥션 대기 → 풀 크기 부족
  hikaricp_connections_active       포화 여부
  resilience4j_circuitbreaker_state 외부 연동 차단 여부
  burty_outbox_dead_total           아웃박스 적체
  jvm_memory_used_bytes             힙 압박
`,
  };
}
