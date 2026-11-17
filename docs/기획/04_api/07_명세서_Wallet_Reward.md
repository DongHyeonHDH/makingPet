# 07. API 명세서 — Wallet · Reward

> 베이스 경로: `/api/v1/wallet/**`, `/api/v1/rewards/**` · 패키지: `wallet`, `reward`
> 규칙 근거: `06_features/02_게임_시스템_규칙.md` §4, §5. "오늘"은 KST 날짜.

| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| W-1 | GET | /api/v1/wallet | ✅ | 잔액 |
| W-2 | GET | /api/v1/wallet/transactions | ✅ (M2) | 코인 내역 |
| R-1 | GET | /api/v1/rewards/today | ✅ | 오늘 현황 |
| R-2 | POST | /api/v1/rewards/attendance | ✅ | 출석 |
| R-3 | GET | /api/v1/rewards/attendance | ✅ | 출석 달력 |
| R-4 | POST | /api/v1/rewards/rps | ✅ | 가위바위보 |
| R-5 | POST | /api/v1/rewards/slot | ✅ 🔑 | 슬롯 |

---

## W-1. GET `/api/v1/wallet`
### 응답 200
```json
{ "data": { "balance": 1230 } }
```

## W-2. GET `/api/v1/wallet/transactions` (M2)
| query | 비고 |
|---|---|
| page, size | 0, 20 |
| reason | 선택. `SIGNUP_GIFT, ATTENDANCE, RPS, SLOT_ENTRY, SLOT_PAYOUT, GACHA, GACHA_REFUND, AFFECTION_REWARD, SHOP_PURCHASE, AUCTION_BUY, AUCTION_SELL, AUCTION_FEE, ADMIN_ADJUST` |
| from, to | 날짜 (선택) |
### 응답 200: `PageResponse<Transaction>` (최신순 고정)
```json
{ "id": "uuid", "amount": -100, "balanceAfter": 1130, "reason": "GACHA", "refType": "GACHA_BATCH", "refId": "uuid", "memo": null, "createdAt": "..." }
```

---

## R-1. GET `/api/v1/rewards/today`
### 응답 200
```json
{ "data": { "date": "2026-11-16",
            "attendance": { "claimed": true, "streak": 3, "nextBonusIn": 4 },
            "rps": { "claimed": false },
            "slot": { "used": 2, "limit": 5, "remaining": 3, "entryFee": 20 },
            "resetsAt": "2026-11-17T00:00:00+09:00" } }
```

---

## R-2. POST `/api/v1/rewards/attendance`
**설명**: 오늘 출석. 어제 출석했으면 streak+1, 아니면 1. 7일째 보너스. `reward_claims (member_id, type, reward_date)` 유니크로 중복 차단.
### 요청: body 없음
### 응답 201
```json
{ "success": true, "message": "출석 완료! 30코인을 받았습니다.",
  "data": { "date": "2026-11-16", "streak": 7, "coins": 30, "bonusCoins": 100, "wallet": { "balance": 1360 } } }
```
### 에러
| HTTP | code | 발생 조건 |
|---|---|---|
| 409 | REWARD_ALREADY_CLAIMED | 오늘 이미 출석 |

## R-3. GET `/api/v1/rewards/attendance`
| query | 비고 |
|---|---|
| month | `2026-11` (기본 이번 달) |
### 응답 200
```json
{ "data": { "month": "2026-11", "days": [ "2026-11-14", "2026-11-15", "2026-11-16" ], "streak": 3, "totalThisMonth": 3 } }
```

---

## R-4. POST `/api/v1/rewards/rps`
**설명**: 하루 1회. 서버가 상대 손을 결정하고 판정·보상을 한 트랜잭션으로 처리. 유니크 제약으로 중복 차단.
### 요청
```json
{ "hand": "ROCK" }
```
| 필드 | 값 |
|---|---|
| hand | `ROCK` / `PAPER` / `SCISSORS` |
### 응답 201
```json
{ "success": true, "message": "이겼습니다! 50코인",
  "data": { "myHand": "ROCK", "opponentHand": "SCISSORS", "result": "WIN", "coins": 50, "wallet": { "balance": 1410 } } }
```
`result`: `WIN`(50) / `DRAW`(20) / `LOSE`(10)
### 에러: 409 `REWARD_ALREADY_CLAIMED`

---

## R-5. POST `/api/v1/rewards/slot`
**설명**: 하루 5회, 참가비 20. 한 트랜잭션: 일일 카운터 행 잠금·증가 → 참가비 차감(`SLOT_ENTRY`) → 결과 등급 추첨 → 심볼 배치 생성 → 배당 지급(`SLOT_PAYOUT`) → 기록.
**헤더**: `Idempotency-Key` 필수
### 요청: body 없음 (`{}` 허용)
### 응답 201
```json
{ "success": true, "message": "딸기 3개! 80코인",
  "data": { "reels": [ "STRAWBERRY", "STRAWBERRY", "STRAWBERRY" ], "tier": "TRIPLE_STRAWBERRY",
            "entryFee": 20, "payout": 80, "net": 60,
            "remaining": 2, "wallet": { "balance": 1470 } } }
```
| 필드 | 값 |
|---|---|
| reels[] | `DUCK` / `CAT` / `DOG` / `DINO` / `STRAWBERRY` / `STAR` |
| tier | `JACKPOT`(⭐⭐⭐ 500) / `TRIPLE_PET`(150) / `TRIPLE_STRAWBERRY`(80) / `PAIR`(30) / `NONE`(0) |
클라이언트는 릴 연출을 하되 멈추는 심볼은 `reels` 값으로만 정한다.
### 에러
| HTTP | code | 발생 조건 |
|---|---|---|
| 429 | REWARD_DAILY_LIMIT_EXCEEDED | 5회 소진. `Retry-After`는 자정까지 초 |
| 400 | WALLET_INSUFFICIENT_BALANCE | 20코인 미만 |
| 400 | IDEMPOTENCY_KEY_REQUIRED | |
