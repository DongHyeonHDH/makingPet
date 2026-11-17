# 09. API 명세서 — Auction (M2)

> 베이스 경로: `/api/v1/auction/**` · 패키지: `auction`
> 규칙 근거: `06_features/02_게임_시스템_규칙.md` §7. MVP는 **즉시구매형**. 입찰식(A-10 기능)은 M3 COULD로 이 문서에 없다.

| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| A-1 | GET | /api/v1/auction/listings | 🔓 | 검색 |
| A-2 | GET | /api/v1/auction/listings/{listingId} | 🔓 | 상세 |
| A-3 | POST | /api/v1/auction/listings | ✅ 🔑 | 등록 |
| A-4 | POST | /api/v1/auction/listings/{listingId}/purchases | ✅ 🔑 | 즉시구매 |
| A-5 | DELETE | /api/v1/auction/listings/{listingId} | ✅ | 취소 |
| A-6 | GET | /api/v1/auction/my-listings | ✅ | 내 매물 |
| A-7 | GET | /api/v1/auction/my-trades | ✅ | 내 거래 |
| A-8 | PUT | /api/v1/auction/listings/{listingId}/favorite | ✅ | 찜 |
| A-9 | DELETE | /api/v1/auction/listings/{listingId}/favorite | ✅ | 찜 해제 |
| A-10 | GET | /api/v1/auction/favorites | ✅ | 내 찜 |
| A-11 | GET | /api/v1/auction/prices | 🔓 | 시세 |

**Listing 응답 조각**
```json
{ "id": "uuid", "item": { "...ItemSummary" }, "seller": { "id": "uuid", "nickname": "오리사랑" },
  "quantity": 1, "price": 55, "status": "ON_SALE",
  "expiresAt": "2026-11-17T23:30:00+09:00", "remainingSeconds": 86200,
  "favorited": false, "isMine": false, "createdAt": "..." }
```
`favorited`, `isMine`은 로그인 시만 계산(익명이면 false).

---

## A-1. GET `/api/v1/auction/listings`
| query | 타입 | 기본 | 비고 |
|---|---|---|---|
| q | string | | 아이템 이름 부분 일치 (스냅샷 `item_name`) |
| category | enum | | TOP / BOTTOM / ACCESSORY / FURNITURE / THEME |
| itemId | uuid | | 특정 아이템만 (시세 화면에서 진입) |
| maxPrice | int | | |
| sort | string | `createdAt,desc` | 화이트리스트: `price,asc` `price,desc` `expiresAt,asc` `createdAt,desc` |
| page, size | | 0, 20 | |
`ON_SALE`이고 `expires_at > now`인 매물만.
### 응답 200: `PageResponse<Listing>`
### 에러: 400 `SORT_KEY_NOT_ALLOWED`, 400 `PAGINATION_SIZE_TOO_LARGE`

## A-2. GET `/api/v1/auction/listings/{listingId}`
### 응답 200: Listing + `priceInfo: { "lowestOnSale": 50, "avg7d": 58, "trades7d": 12 }`
만료·종료된 매물도 조회된다(상태 표시). 404는 없는 ID만.

---

## A-3. POST `/api/v1/auction/listings`
**설명**: 한 트랜잭션: 거래 가능 확인 → `item.withdraw(itemId, 1)`(에스크로: quantity −1) → 매물 생성.
**헤더**: `Idempotency-Key` 필수
### 요청
```json
{ "itemId": "uuid", "price": 55, "durationHours": 24 }
```
| 필드 | 제약 |
|---|---|
| itemId | tradable 아이템 |
| price | 1~99,999 |
| durationHours | 12 / 24 / 48 |
### 응답 201: Listing (`isMine: true`)
### 에러
| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | ITEM_NOT_TRADABLE | MOTION 등 |
| 400 | INVENTORY_INSUFFICIENT | available < 1 |
| 404 | ITEM_NOT_FOUND | |
| 409 | AUCTION_LISTING_LIMIT_EXCEEDED | ON_SALE 10건 초과 |

---

## A-4. POST `/api/v1/auction/listings/{listingId}/purchases`
**설명**: 즉시구매. 규칙 §7-5 순서 그대로, 한 트랜잭션.
**헤더**: `Idempotency-Key` 필수
### 요청: body 없음
### 응답 201
```json
{ "success": true, "message": "딸기 리본을 구매했습니다.",
  "data": { "tradeId": "uuid", "listing": { "...Listing(status SOLD)" },
            "price": 55, "fee": 3, "sellerIncome": 52,
            "inventory": { "quantity": 1, "available": 1 }, "wallet": { "balance": 1275 } } }
```
### 처리 순서 (서버)
1. 매물 `SELECT ... FOR UPDATE`. 상태 ON_SALE·`expires_at > now` 아니면 409.
2. 구매자 == 판매자면 400.
3. `wallet.debit(buyer, price, AUCTION_BUY, listingId)` → 부족 시 400.
4. `wallet.credit(seller, price − fee, AUCTION_SELL, listingId)`, fee = ceil(price × 0.05), 최소 1. 수수료는 원장에 `AUCTION_FEE`로 판매자 기준 −fee 기록(총량 추적용).
5. `item.grant(buyer, itemId, 1)`.
6. 매물 SOLD, `closed_at`, `auction_trades` 생성.
- 지갑 잠금 순서: member_id 오름차순(데드락 방지).
### 에러
| HTTP | code | 발생 조건 |
|---|---|---|
| 409 | AUCTION_LISTING_NOT_ON_SALE | 이미 판매·취소·만료 (동시 구매 패자 포함) |
| 400 | AUCTION_SELF_PURCHASE | 본인 매물 |
| 400 | WALLET_INSUFFICIENT_BALANCE | |
| 404 | AUCTION_LISTING_NOT_FOUND | |
### 부수효과 (M3)
판매자에게 SSE `AUCTION_SOLD` 이벤트.

---

## A-5. DELETE `/api/v1/auction/listings/{listingId}`
**설명**: ON_SALE → CANCELLED, 아이템 반환(`item.grant(seller, itemId, 1)`). 수수료 없음.
### 응답 200: Listing (CANCELLED)
### 에러
| HTTP | code | 발생 조건 |
|---|---|---|
| 403 | AUCTION_LISTING_NOT_OWNED | 남의 매물 |
| 409 | AUCTION_LISTING_NOT_ON_SALE | 이미 종료 |

---

## A-6. GET `/api/v1/auction/my-listings`
| query | 비고 |
|---|---|
| status | `ON_SALE`(기본) / `SOLD` / `CANCELLED` / `EXPIRED` / `REMOVED` / `ALL` |
| page, size | |
### 응답 200: `PageResponse<Listing>` 최신순. SOLD에는 `trade: { buyer, price, fee, sellerIncome, tradedAt }` 포함.

## A-7. GET `/api/v1/auction/my-trades`
| query | 비고 |
|---|---|
| role | `BUY` / `SELL` / `ALL`(기본) |
| page, size | |
### 응답 200: `PageResponse<Trade>`
```json
{ "id": "uuid", "role": "BUY", "item": { "...ItemSummary" }, "counterparty": { "nickname": "..." },
  "price": 55, "fee": 0, "net": -55, "tradedAt": "..." }
```
`net`: BUY면 −price, SELL이면 price − fee.

---

## A-8. PUT `/api/v1/auction/listings/{listingId}/favorite`
멱등. 이미 찜이면 200. ON_SALE이 아니어도 허용(기록 유지).
### 응답 200 `{ "data": { "favorited": true } }`

## A-9. DELETE `/api/v1/auction/listings/{listingId}/favorite`
### 응답 200 `{ "data": { "favorited": false } }`

## A-10. GET `/api/v1/auction/favorites`
### 응답 200: `PageResponse<Listing>`. 종료된 매물도 상태와 함께 보여주고, 클라이언트가 구분 표시.

---

## A-11. GET `/api/v1/auction/prices`
| query | 비고 |
|---|---|
| itemId | 선택. 없으면 거래 가능한 전 아이템 |
| category | 선택 |
### 응답 200
```json
{ "data": { "items": [
  { "item": { "...ItemSummary" }, "lowestOnSale": 50, "onSaleCount": 4, "avg7d": 58, "trades7d": 12, "last": { "price": 55, "tradedAt": "..." } } ] } }
```
매물이 없으면 `lowestOnSale: null`. 7일 체결이 없으면 `avg7d: null`. 응답은 60초 캐시 가능.

---

## 스케줄러 (엔드포인트 아님)
- 1분마다: `ON_SALE AND expires_at <= now` → EXPIRED, `item.grant(seller, itemId, 1)`, (M3) SSE `AUCTION_EXPIRED`.
- 매물 1건씩 트랜잭션. 구매와 경합하면 `FOR UPDATE SKIP LOCKED`.
