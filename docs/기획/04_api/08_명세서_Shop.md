# 08. API 명세서 — Shop

> 베이스 경로: `/api/v1/shop/**` · 패키지: `shop`
> 규칙 근거: `06_features/02_게임_시스템_규칙.md` §6

| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| S-1 | GET | /api/v1/shop/products | 🔓 | 상품 목록 |
| S-2 | GET | /api/v1/shop/products/{productId} | 🔓 | 상품 상세 |
| S-3 | POST | /api/v1/shop/products/{productId}/purchases | ✅ 🔑 | 구매 |

---

## S-1. GET `/api/v1/shop/products`
| query | 비고 |
|---|---|
| category | 선택 |
| page, size | 0, 50 |
현재 시각에 판매 중(`visible` 이고 기간 안)인 상품만. 정렬 `sort_order`.
### 응답 200: `PageResponse<Product>`
```json
{ "id": "uuid", "item": { "...ItemSummary" }, "price": 70,
  "saleEndsAt": null, "owned": 2 }
```
`owned`: 로그인한 경우 보유 수량, 익명이면 null.

## S-2. GET `/api/v1/shop/products/{productId}`
### 응답 200: Product + `item.description`
### 에러: 404 `SHOP_PRODUCT_NOT_FOUND`

---

## S-3. POST `/api/v1/shop/products/{productId}/purchases`
**설명**: 한 트랜잭션: 판매 중 확인 → `wallet.debit(price×quantity, SHOP_PURCHASE)` → `item.grant(itemId, quantity)` → 구매 기록.
**헤더**: `Idempotency-Key` 필수
### 요청
```json
{ "quantity": 2 }
```
| 필드 | 제약 |
|---|---|
| quantity | 1~10, 기본 1 |
### 응답 201
```json
{ "success": true, "message": "딸기 리본 2개를 구매했습니다.",
  "data": { "purchaseId": "uuid", "item": { "...ItemSummary" }, "quantity": 2, "unitPrice": 70, "totalPrice": 140,
            "inventory": { "quantity": 4, "available": 3 }, "wallet": { "balance": 1330 } } }
```
### 에러
| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | VALIDATION_FAILED | quantity 범위 밖 |
| 400 | WALLET_INSUFFICIENT_BALANCE | |
| 404 | SHOP_PRODUCT_NOT_FOUND | |
| 409 | SHOP_PRODUCT_NOT_ON_SALE | 기간 외·비노출 |
