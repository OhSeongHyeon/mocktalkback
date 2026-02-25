# Refresh Token Race Condition 재현 가이드 (k6 전용)

이 문서는 Redis Lua 기반 refresh 원자성 검증을 `k6`만으로 수행하는 절차입니다.

## 1) 대상 스크립트

- `mocktalkback/perf/k6/refresh.race.load.js`
- 동작:
  - `setup()`에서 로그인 후 동일 `refresh_token`을 확보
  - `K6_REFRESH_BURST` 수만큼 동시 `/api/auth/refresh` 요청
  - `refresh_status_200/401/403/other` 메트릭 집계

## 2) 재현 모드 설정 (non-atomic)

서버 환경변수:

```env
AUTH_REFRESH_ROTATE_MODE=non-atomic
AUTH_REFRESH_REPRO_DELAY_MS=150
```

- `non-atomic`: 재현용 비원자 경로
- `AUTH_REFRESH_REPRO_DELAY_MS`: 검증-갱신 사이 지연으로 경합 노출 강화

## 3) k6 실행

프로젝트 루트(`mocktalkback`)에서 실행:

```bash
k6 run perf/k6/refresh.race.load.js \
  -e BASE_URL=http://localhost:8082 \
  -e K6_LOGIN_ID=YOUR_ID \
  -e K6_PASSWORD=YOUR_PASSWORD \
  -e K6_ORIGIN=http://localhost:5173 \
  -e K6_REFRESH_BURST=30
```

예시:

```bash
k6 run perf/k6/refresh.race.load.js -e BASE_URL=http://localhost:8082 -e K6_LOGIN_ID=admin -e K6_PASSWORD=123123123 -e K6_ORIGIN=http://localhost:5173 -e K6_REFRESH_BURST=30
```

## 4) 결과 해석

- 재현 모드(non-atomic): `refresh_status_200`이 다건이면 race condition 재현 성공
- 원자 모드(atomic): 보통 `refresh_status_200=1`, 나머지 `refresh_status_401`
- 참고: `http_req_failed`는 `401`을 실패로 집계하므로 이 시나리오 판단 지표는 아닙니다.

## 5) 원자 모드 검증

서버 환경변수 원복:

```env
AUTH_REFRESH_ROTATE_MODE=atomic
AUTH_REFRESH_REPRO_DELAY_MS=0
```

동일 k6 명령 재실행 후 `200 1건 / 401 나머지` 패턴을 확인합니다.

## 리프레쉬 토큰 동시성 & 원자성 검증 결과

비원자성 Redis Lua 미적용
```bash

         /\      Grafana   /‾‾/
    /\  /  \     |\  __   /  /
   /  \/    \    | |/ /  /   ‾‾\
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/


     execution: local
        script: perf/k6/refresh.race.load.js
        output: -

     scenarios: (100.00%) 1 scenario, 30 max VUs, 1m0s max duration (incl. graceful stop):
              * refresh_race_burst: 1 iterations for each of 30 VUs (maxDuration: 30s, exec: refreshRaceScenario, gracefulStop: 30s)



  █ THRESHOLDS

    refresh_unexpected_status
    ✓ 'rate==0' rate=0.00%


  █ TOTAL RESULTS

    checks_total.......: 1       1.35011/s
    checks_succeeded...: 100.00% 1 out of 1
    checks_failed......: 0.00%   0 out of 1

    ✓ refresh race login status 200

    CUSTOM
    refresh_requests...............: 30     40.503299/s
    refresh_status_200.............: 10     13.5011/s
    refresh_status_401.............: 20     27.0022/s
    refresh_unexpected_status......: 0.00%  0 out of 30

    HTTP
    http_req_duration..............: avg=251.27ms min=234.67ms med=246.64ms max=476.39ms p(90)=249.45ms p(95)=250.52ms
      { expected_response:true }...: avg=257.83ms min=234.67ms med=236.46ms max=476.39ms p(90)=237.21ms p(95)=356.8ms
    http_req_failed................: 64.51% 20 out of 31
    http_reqs......................: 31     41.853409/s

    EXECUTION
    iteration_duration.............: avg=245.38ms min=235.96ms med=247.92ms max=251.43ms p(90)=251.15ms p(95)=251.15ms
    iterations.....................: 30     40.503299/s

    NETWORK
    data_received..................: 27 kB  37 kB/s
    data_sent......................: 15 kB  21 kB/s




running (0m00.7s), 00/30 VUs, 30 complete and 0 interrupted iterations
refresh_race_burst ✓ [======================================] 30 VUs  00.3s/30s  30/30 iters, 1 per VU
```

원자성 Redis Lua 적용
```bash

         /\      Grafana   /‾‾/
    /\  /  \     |\  __   /  /
   /  \/    \    | |/ /  /   ‾‾\
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/


     execution: local
        script: perf/k6/refresh.race.load.js
        output: -

     scenarios: (100.00%) 1 scenario, 30 max VUs, 1m0s max duration (incl. graceful stop):
              * refresh_race_burst: 1 iterations for each of 30 VUs (maxDuration: 30s, exec: refreshRaceScenario, gracefulStop: 30s)



  █ THRESHOLDS

    refresh_unexpected_status
    ✓ 'rate==0' rate=0.00%


  █ TOTAL RESULTS

    checks_total.......: 1       1.648049/s
    checks_succeeded...: 100.00% 1 out of 1
    checks_failed......: 0.00%   0 out of 1

    ✓ refresh race login status 200

    CUSTOM
    refresh_requests...............: 30     49.441484/s
    refresh_status_200.............: 1      1.648049/s
    refresh_status_401.............: 29     47.793435/s
    refresh_unexpected_status......: 0.00%  0 out of 30

    HTTP
    http_req_duration..............: avg=104.08ms min=86.52ms  med=91.03ms  max=484.1ms  p(90)=95.46ms  p(95)=100.51ms
      { expected_response:true }...: avg=294.83ms min=105.56ms med=294.83ms max=484.1ms  p(90)=446.25ms p(95)=465.18ms
    http_req_failed................: 93.54% 29 out of 31
    http_reqs......................: 31     51.089534/s

    EXECUTION
    iteration_duration.............: avg=93.05ms  min=88.37ms  med=92.11ms  max=106.57ms p(90)=96.55ms  p(95)=97.1ms
    iterations.....................: 30     49.441484/s

    NETWORK
    data_received..................: 17 kB  29 kB/s
    data_sent......................: 15 kB  25 kB/s




running (0m00.6s), 00/30 VUs, 30 complete and 0 interrupted iterations
refresh_race_burst ✓ [======================================] 30 VUs  00.1s/30s  30/30 iters, 1 per VU
```