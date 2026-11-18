# 05. API 명세서 — Gacha

> 베이스 경로: `/api/v1/gacha/**` · 패키지: `gacha`
> 규칙 근거: `06_features/02_게임_시스템_규칙.md` §1

| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| G-1 | GET | /api/v1/gacha/pools | 🔓 | 활성 풀 + 확률표 |
| G-2 | POST | /api/v1/gacha/pools/{poolId}/draws | ✅ 🔑 | 뽑기 |
| G-3 | GET | /api/v1/gacha/draws | ✅ (M2) | 내 기록 |
| G-4 | GET | /api/v1/gacha/pools/{poolId}/pity | — | 천장 카운터 — 폐지 (D-13) |

---

## G-1. GET `/api/v1/gacha/pools`
**설명**: 현재 시각에 활성인 풀만. 확률은 종 단위까지 공개.
### 응답 200
```json
{ "data": { "items": [
  { "id": "uuid", "name": "상시 뽑기", "type": "STANDARD",
    "singlePrice": 100, "multiPrice": 900,
    "startsAt": null, "endsAt": null,
    "rates": [
      { "rarity": "COMMON", "rate": 1.0, "species": [ { "id": "uuid", "code": "duck", "name": "오리", "rate": 0.25 }, { "...": "..." } ] } ] } ] } }
```
`rate`는 소수(합계 1.0). 서버 내부는 만분율 가중치.
D-13: 상시·한정 풀 모두 기본종(1단계, COMMON)만 담는다. 진화형은 풀 엔트리로 등록할 수 없다(400).

---

## G-2. POST `/api/v1/gacha/pools/{poolId}/draws`
**설명**: 1회 또는 10연 뽑기. 한 트랜잭션: 코인 차감 → `count`회 추첨(기본종 가중치) → 회차마다 새 펫 생성 또는 코인 환급 → 기록·천장 카운터 갱신. 첫 펫이면 자동 대표 펫.
**인증**: USER · **헤더**: `Idempotency-Key` 필수
### 요청
```json
{ "count": 10 }
```
| 필드 | 값 |
|---|---|
| count | 1 또는 10 |
### 응답 201
```json
{ "success": true, "message": "새 친구를 만났습니다!",
  "data": { "batchId": "uuid", "poolId": "uuid", "cost": 900,
            "results": [
              { "order": 1, "resultType": "NEW_PET", "species": { "id": "uuid", "code": "cat", "name": "고양이", "rarity": "COMMON", "assetKey": "..." },
                "pet": { "...PetSummary" }, "refundCoins": 0, "pityTriggered": false, "guaranteed": false },
              { "order": 2, "resultType": "REFUND", "species": { "code": "duck", "rarity": "COMMON", "...": "..." },
                "pet": null, "refundCoins": 20, "pityTriggered": false, "guaranteed": false } ],
            "summary": { "newPets": 3, "refunds": 7, "refundCoins": 160 },
            "wallet": { "balance": 490 } } }
```
| 필드 | 설명 |
|---|---|
| resultType | `NEW_PET` / `REFUND` (이미 보유한 종, D-8) |
| guaranteed | D-13으로 폐지. 항상 `false` (클라이언트 호환을 위해 v1.1까지 필드 유지) |
| pityTriggered | D-13으로 폐지. 항상 `false` |
| summary | 연출용 집계 |
### 처리 순서 (서버)
1. 멱등 키 확인. 2. 풀 활성·기간 확인. 3. `wallet.debit(cost, GACHA, batchId)` — 실패 시 400.
4. 회차별: 기본종 가중치로 추첨한다 (천장·10연 보장 없음, D-13).
5. 결과 종이 `user_pets`에 (삭제되지 않은 채로) 있으면 `wallet.credit(refund, GACHA_REFUND)`, 없으면 펫 생성. **같은 배치 안에서 앞 회차가 만든 펫도 보유로 친다.**
6. 모든 회차를 `gacha_draws`에 기록.
### 에러
| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | VALIDATION_FAILED | count가 1·10이 아님 |
| 400 | IDEMPOTENCY_KEY_REQUIRED | |
| 400 | WALLET_INSUFFICIENT_BALANCE | |
| 404 | GACHA_POOL_NOT_FOUND | |
| 409 | GACHA_POOL_INACTIVE | 기간 외·비활성 |
| 409 | IDEMPOTENCY_KEY_REUSED | 같은 키, 다른 body |

---

## G-3. GET `/api/v1/gacha/draws` (M2)
| query | 기본 | 비고 |
|---|---|---|
| page, size | 0, 20 | |
| poolId | | 선택 |
### 응답 200: `PageResponse<DrawRecord>`
```json
{ "id": "uuid", "batchId": "uuid", "poolName": "상시 뽑기", "species": { "code": "cat", "name": "고양이", "rarity": "COMMON" },
  "resultType": "NEW_PET", "refundCoins": 0, "pityTriggered": false, "createdAt": "..." }
```
정렬 고정: `createdAt,desc`.

---

## G-4. GET `/api/v1/gacha/pools/{poolId}/pity` — 폐지 (D-13)
### 응답 200
```json
{ "data": { "poolId": "uuid", "count": 12, "threshold": 50, "guarantee": "EPIC" } }
```
