# 10. API 명세서 — Community · Admin

> 베이스 경로: `/api/v1/notices/**`, `/api/v1/admin/**` · 패키지: `community`, `admin`
> 관리자 API는 전부 `role ∈ {ADMIN, SUPER_ADMIN}`. 👑 표시는 SUPER_ADMIN만. 권한 부족 시 403 `FORBIDDEN`.
> 초기(M1)에는 AD-1~AD-5, AD-8~AD-9를 SQL 시드(Flyway `R__seed.sql`)로 대체해도 된다. 엔드포인트는 만들되 화면은 M3.

## Community
| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| C-1 | GET | /api/v1/notices | 🔓 | 목록 |
| C-2 | GET | /api/v1/notices/{noticeId} | 🔓 | 상세 |

### C-1. GET `/api/v1/notices`
| query | 기본 |
|---|---|
| page, size | 0, 10 |
정렬: `pinned desc, publishedAt desc`. `published_at <= now`이고 삭제되지 않은 것만.
### 응답 200: `PageResponse<NoticeSummary>`
```json
{ "id": "uuid", "title": "새 친구 '공룡' 출시", "pinned": true, "publishedAt": "..." }
```
### C-2. GET `/api/v1/notices/{noticeId}`
### 응답 200: NoticeSummary + `content`(마크다운 문자열), `author: { nickname }`
### 에러: 404 `NOTICE_NOT_FOUND`

---

## Admin — 카탈로그
| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| AD-1 | POST | /api/v1/admin/pet-species | 🛡 | 펫 종 등록 |
| AD-2 | PATCH | /api/v1/admin/pet-species/{speciesId} | 🛡 | 수정 |
| AD-3 | PUT | /api/v1/admin/pet-species/{speciesId}/motions | 🛡 | 해금 움직임 매핑 |
| AD-4 | POST | /api/v1/admin/items | 🛡 | 아이템 등록 |
| AD-5 | PATCH | /api/v1/admin/items/{itemId} | 🛡 | 수정 |

### AD-1. POST `/api/v1/admin/pet-species`
```json
{ "code": "dino", "name": "공룡", "rarity": "EPIC", "baseAssetKey": "pets/dino/base.png", "description": "...", "active": true }
```
| 필드 | 제약 |
|---|---|
| code | `^[a-z][a-z0-9_]{1,39}$`, 유니크, **변경 불가** |
| rarity | COMMON / RARE / EPIC / LEGENDARY |
### 응답 201: Species 전체. 에러: 409 `PET_SPECIES_CODE_DUPLICATED`

### AD-2. PATCH — `name`, `baseAssetKey`, `description`, `active` 중 보낸 것만. `rarity`는 **변경 불가**(기존 뽑기 확률·환급액 기록과 충돌) → 바꾸려면 새 종.

### AD-3. PUT `/api/v1/admin/pet-species/{speciesId}/motions`
```json
{ "motions": [ { "affectionLevel": 2, "motionItemId": "uuid" }, { "affectionLevel": 3, "motionItemId": "uuid" } ] }
```
affectionLevel 2~5. 전체 교체. 아이템은 MOTION 카테고리여야 함(400 `ITEM_SLOT_MISMATCH`).

### AD-4. POST `/api/v1/admin/items`
```json
{ "code": "ribbon", "name": "딸기 리본", "category": "ACCESSORY", "rarity": "COMMON", "assetKey": "items/ribbon.png", "tradable": true, "active": true }
```
- `category = MOTION`이면 `tradable`은 강제로 false.
### 응답 201: ItemSummary. 에러: 409 `ITEM_CODE_DUPLICATED`

### AD-5. PATCH — `name`, `assetKey`, `tradable`, `active`. `category`, `rarity` 변경 불가.

---

## Admin — 뽑기 풀 (M2)
| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| AD-6 | POST | /api/v1/admin/gacha/pools | 🛡 | 생성 |
| AD-7 | PATCH | /api/v1/admin/gacha/pools/{poolId} | 🛡 | 활성/기간 |

### AD-6. POST `/api/v1/admin/gacha/pools`
```json
{ "name": "공룡 픽업", "type": "LIMITED", "singlePrice": 100, "multiPrice": 900,
  "pityThreshold": 50, "startsAt": "2026-12-01T00:00:00+09:00", "endsAt": "2026-12-14T23:59:59+09:00",
  "entries": [ { "speciesId": "uuid", "weight": 3000 }, { "speciesId": "uuid", "weight": 7000 } ] }
```
| 검증 | 에러 |
|---|---|
| weight 합계 = 10000 | 400 `GACHA_WEIGHT_SUM_INVALID` |
| RARE 이상 종 1개 이상 포함 (10연 보장 가능) | 400 `GACHA_POOL_GUARANTEE_IMPOSSIBLE` |
| EPIC 이상 종 1개 이상 포함 (천장 가능) | 400 `GACHA_POOL_GUARANTEE_IMPOSSIBLE` |
| 종 active=true | 400 `PET_SPECIES_INACTIVE` |
### 응답 201: G-1의 풀 1건 형식.

### AD-7. PATCH — `active`, `startsAt`, `endsAt`, `name`만. `entries`를 보내면 409 `GACHA_POOL_ENTRIES_IMMUTABLE`. 확률을 바꾸려면 새 풀.

---

## Admin — 상점 · 공지
| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| AD-8 | POST | /api/v1/admin/shop/products | 🛡 | 상품 등록 |
| AD-9 | PATCH | /api/v1/admin/shop/products/{productId} | 🛡 | 수정 |
| AD-10 | POST | /api/v1/admin/notices | 🛡 | 공지 작성 |
| AD-11 | PATCH | /api/v1/admin/notices/{noticeId} | 🛡 | 수정·고정 |
| AD-12 | DELETE | /api/v1/admin/notices/{noticeId} | 🛡 | 삭제(soft) |

### AD-8
```json
{ "itemId": "uuid", "price": 70, "saleStartsAt": null, "saleEndsAt": null, "visible": true, "sortOrder": 10 }
```
`price` 1~99,999. MOTION 아이템은 등록 불가(400 `ITEM_NOT_TRADABLE`).
### AD-9 — `price`, `saleStartsAt`, `saleEndsAt`, `visible`, `sortOrder`.
### AD-10
```json
{ "title": "...", "content": "마크다운", "pinned": false, "publishedAt": null }
```
`publishedAt` null이면 즉시 게시. title 1~100.
### AD-11 — 보낸 필드만. AD-12 — 200, `data` 없음.

---

## Admin — 회원 · 매물 (M2~M3)
| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| AD-13 | POST | /api/v1/admin/members/{memberId}/coin-adjustments | 👑 | 재화 보정 |
| AD-14 | PATCH | /api/v1/admin/members/{memberId}/role | 👑 | 권한 변경 |
| AD-15 | DELETE | /api/v1/admin/auction/listings/{listingId} | 🛡 | 매물 강제 내림 |

### AD-13
```json
{ "amount": -500, "memo": "버그 보상 회수 (이슈 #42)" }
```
| 필드 | 제약 |
|---|---|
| amount | 0 아님, ±1,000,000 이내 |
| memo | 필수, 5~200자 |
원장 `ADMIN_ADJUST`, `memo`에 사유, `ref_id`에 관리자 member_id. 음수 보정으로 잔액이 부족하면 400 `WALLET_INSUFFICIENT_BALANCE`(음수 잔액 금지).
### 응답 201 `{ "data": { "memberId": "uuid", "amount": -500, "wallet": { "balance": 730 } } }`

### AD-14
```json
{ "role": "ADMIN" }
```
본인 권한 변경 403 `MEMBER_SELF_ROLE_CHANGE`. SUPER_ADMIN 부여는 허용하되 로그.

### AD-15 — ON_SALE → REMOVED, 아이템 판매자에게 반환. body `{ "reason": "..." }` 필수. 에러 409 `AUCTION_LISTING_NOT_ON_SALE`.
