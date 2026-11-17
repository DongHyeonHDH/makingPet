# 06. API 명세서 — Item · Inventory · Home

> 베이스 경로: `/api/v1/items`, `/api/v1/inventory`, `/api/v1/home/**`, `/api/v1/homes/**` · 패키지: `item`, `home`
> 규칙 근거: `06_features/02_게임_시스템_규칙.md` §3

| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| I-1 | GET | /api/v1/items | 🔓 | 아이템 카탈로그 |
| I-2 | GET | /api/v1/inventory | ✅ | 내 인벤토리 |
| H-1 | GET | /api/v1/home | ✅ | 내 마이 홈 |
| H-2 | PUT | /api/v1/home/theme | ✅ | 테마 변경 |
| H-3 | PUT | /api/v1/home/furniture | ✅ (M2) | 가구 배치 교체 |
| H-4 | PATCH | /api/v1/home/visibility | ✅ (M2) | 공개 설정 |
| H-5 | GET | /api/v1/homes/{nickname} | 🔓 (M2) | 남의 마이 홈 |
| H-6 | POST | /api/v1/homes/{nickname}/likes | ✅ (M3) | 좋아요 |

---

## I-1. GET `/api/v1/items`
| query | 비고 |
|---|---|
| category | TOP / BOTTOM / ACCESSORY / MOTION / FURNITURE / THEME (선택) |
| page, size | 기본 0, 50 |
### 응답 200: `PageResponse<ItemSummary>`. `active=false`는 제외.

---

## I-2. GET `/api/v1/inventory`
| query | 비고 |
|---|---|
| category | 선택 |
### 응답 200
```json
{ "data": { "items": [
  { "item": { "...ItemSummary" }, "quantity": 3, "reserved": 1, "listing": 1, "available": 1 } ] } }
```
| 필드 | 설명 |
|---|---|
| quantity | 보유 수량 (경매 등록분은 이미 빠져 있음) |
| reserved | 장착·배치 중 |
| listing | 경매 ON_SALE 등록 중 (참고용, `auction` 서비스에서 집계) |
| available | `quantity - reserved` = 장착·배치·경매 등록에 쓸 수 있는 수량 |

---

## H-1. GET `/api/v1/home`
### 응답 200
```json
{ "data": { "owner": { "id": "uuid", "nickname": "오리사랑" },
            "visibility": "PUBLIC", "likeCount": 0,
            "representativePet": { "...PetSummary 또는 null" },
            "theme": { "...ItemSummary(THEME)" },
            "furniture": [ { "id": "uuid", "item": { "...ItemSummary(FURNITURE)" }, "x": 40, "y": 70, "layer": 1 } ],
            "furnitureLimit": 20 } }
```

---

## H-2. PUT `/api/v1/home/theme`
### 요청
```json
{ "themeItemId": "uuid" }
```
### 응답 200: H-1 body
### 에러
| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | ITEM_SLOT_MISMATCH | THEME 카테고리가 아님 |
| 400 | INVENTORY_INSUFFICIENT | 보유하지 않음 |
### 부수효과: 이전 테마 `reserved` −1, 새 테마 `reserved` +1.

---

## H-3. PUT `/api/v1/home/furniture` (M2)
**설명**: 배치 전체를 교체한다. 한 트랜잭션에서 기존 배치의 `reserved`를 전부 풀고 새 배치만큼 다시 잡는다.
### 요청
```json
{ "furniture": [ { "itemId": "uuid", "x": 40, "y": 70, "layer": 1 }, { "itemId": "uuid", "x": 10, "y": 20, "layer": 0 } ] }
```
| 필드 | 제약 |
|---|---|
| furniture | 0~20개 |
| x, y | 0~100 정수 |
| layer | 0~99 |
| itemId | FURNITURE 카테고리, 같은 itemId 반복 가능(수량만큼) |
### 응답 200: H-1 body
### 에러
| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | HOME_FURNITURE_LIMIT_EXCEEDED | 20개 초과 |
| 400 | ITEM_SLOT_MISMATCH | |
| 400 | INVENTORY_INSUFFICIENT | 같은 가구를 수량보다 많이 배치 |

---

## H-4. PATCH `/api/v1/home/visibility` (M2)
### 요청
```json
{ "visibility": "PRIVATE" }
```
### 응답 200: H-1 body

---

## H-5. GET `/api/v1/homes/{nickname}` (M2)
**인증**: 없음 (D-11). 본인 닉네임이면 H-1과 동일하게 동작.
### 응답 200: H-1 body에서 `owner`는 MemberPublic만, `furnitureLimit` 제외.
### 에러
| HTTP | code | 발생 조건 |
|---|---|---|
| 404 | HOME_NOT_FOUND | 닉네임 없음·탈퇴 |
| 403 | HOME_PRIVATE | PRIVATE이고 본인이 아님 |

---

## H-6. POST `/api/v1/homes/{nickname}/likes` (M3)
body 없음. `(liker_id, owner_id, like_date)` 유니크.
### 응답 200
```json
{ "data": { "likeCount": 13 } }
```
### 에러: 409 `HOME_ALREADY_LIKED_TODAY`, 400 `HOME_SELF_LIKE`
