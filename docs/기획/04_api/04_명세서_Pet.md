# 04. API 명세서 — Pet

> 베이스 경로: `/api/v1/pets/**` · 패키지: `pet`
> 규칙 근거: `06_features/02_게임_시스템_규칙.md` §2(상호작용·기분·친밀도), §3-1(장착 슬롯)

| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| P-1 | GET | /api/v1/pets | ✅ | 내 펫 목록 |
| P-2 | GET | /api/v1/pets/{petId} | ✅ | 펫 상세 |
| P-3 | PATCH | /api/v1/pets/{petId} | ✅ | 이름 변경 |
| P-4 | PATCH | /api/v1/pets/{petId}/representative | ✅ | 대표 펫 지정 |
| P-5 | POST | /api/v1/pets/{petId}/interactions | ✅ | 상호작용 |
| P-6 | PUT | /api/v1/pets/{petId}/equipment | ✅ | 장착 전체 교체 |
| P-7 | GET | /api/v1/pets/collection | ✅ (M2) | 도감 |
| P-8 | DELETE | /api/v1/pets/{petId} | ✅ (M3) | 놓아주기 |

공통: `{petId}`가 남의 펫이면 403 `PET_NOT_OWNED`, 없거나 삭제됐으면 404 `PET_NOT_FOUND`.
모든 응답의 `mood`는 **조회 시점 기준 지연 계산값**이다(1시간당 −2).

---

## P-1. GET `/api/v1/pets`
### 응답 200
```json
{ "success": true, "message": "조회되었습니다.",
  "data": { "items": [ { "...PetSummary" } ], "representativePetId": "uuid" } }
```
정렬: 대표 펫 → 희귀도 내림차순 → 획득일. 페이지네이션 없음(종당 1마리라 상한이 종 수).

---

## P-2. GET `/api/v1/pets/{petId}`
### 응답 200
```json
{ "data": { "...PetSummary",
            "affection": 45, "affectionLevel": 2, "nextLevelAffection": 100,
            "stage": 1,
            "cooldowns": { "PET": null, "PLAY": "2026-11-16T23:40:00+09:00", "SNACK": null },
            "unlockedMotions": [ { "...ItemSummary(MOTION)" } ],
            "obtainedAt": "..." } }
```
| 필드 | 설명 |
|---|---|
| cooldowns.{type} | 다음 가능 시각. 지금 가능하면 null |
| nextLevelAffection | 다음 단계 필요 누적치. 최고 단계면 null |

---

## P-3. PATCH `/api/v1/pets/{petId}`
### 요청
```json
{ "name": "꽥꽥이" }
```
| 필드 | 제약 |
|---|---|
| name | 1~12자, 앞뒤 공백 제거, 금칙어 |
### 응답 200: PetSummary
### 에러: 400 `PET_NAME_FORBIDDEN_WORD`

---

## P-4. PATCH `/api/v1/pets/{petId}/representative`
body 없음. `members.representative_pet_id`를 바꾼다. 이미 대표면 그대로 200.
### 응답 200: PetSummary (`isRepresentative: true`)

---

## P-5. POST `/api/v1/pets/{petId}/interactions`
**설명**: 상호작용 1회. 쿨다운 검사 → 기분·친밀도 증가 → 단계 상승 시 움직임 아이템 지급·코인 보상 → 쿨다운 갱신. 한 트랜잭션.
### 요청
```json
{ "type": "PLAY" }
```
| 필드 | 값 |
|---|---|
| type | `PET` / `PLAY` / `SNACK` |
### 응답 200
```json
{ "success": true, "message": "꽥꽥이가 신나합니다!",
  "data": { "pet": { "...PetSummary" },
            "motionKey": "motion.play",
            "moodDelta": 15, "affectionDelta": 3,
            "levelUp": { "from": 1, "to": 2, "rewards": { "motions": [ { "...ItemSummary" } ], "coins": 0 } },
            "nextAvailableAt": "2026-11-16T23:50:00+09:00",
            "wallet": { "balance": 1230 } } }
```
| 필드 | 설명 |
|---|---|
| motionKey | 클라이언트가 즉시 재생할 움직임. 종별 해금 움직임이 있으면 그걸 우선 |
| levelUp | 단계가 오르지 않았으면 null |
### 에러
| HTTP | code | 발생 조건 |
|---|---|---|
| 429 | PET_INTERACTION_COOLDOWN | 쿨다운 중. `Retry-After: <초>`, `error.message`에 남은 시간 |
### 동시성
`pet_interaction_cooldowns` 행을 조건부 UPDATE(`WHERE next_available_at <= now`). 영향 0행이면 429.

---

## P-6. PUT `/api/v1/pets/{petId}/equipment`
**설명**: 슬롯 전체를 요청 상태로 교체한다(부분 수정 없음). 해제되는 아이템은 `reserved` 감소, 새로 장착되는 아이템은 `reserved` 증가. 한 트랜잭션.
### 요청
```json
{ "TOP": "item-uuid", "BOTTOM": null, "ACCESSORY_1": "item-uuid", "ACCESSORY_2": null, "MOTION": "item-uuid" }
```
| 슬롯 | 허용 카테고리 |
|---|---|
| TOP | TOP |
| BOTTOM | BOTTOM |
| ACCESSORY_1, ACCESSORY_2 | ACCESSORY (신발 포함, D-12) |
| MOTION | MOTION — 기본 `motion.idle` 또는 해당 **종**에서 해금된 것만 |
- 같은 아이템을 ACCESSORY_1과 ACCESSORY_2에 동시에 넣으려면 거래 가능 수량이 2 이상이어야 한다.
### 응답 200: PetSummary
### 에러
| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | ITEM_SLOT_MISMATCH | 슬롯과 카테고리 불일치 |
| 400 | INVENTORY_INSUFFICIENT | 거래 가능 수량 부족 (장착·배치·경매 중 제외) |
| 400 | PET_MOTION_NOT_UNLOCKED | 이 종에서 해금되지 않은 움직임 |
| 404 | ITEM_NOT_FOUND | |

---

## P-7. GET `/api/v1/pets/collection` (M2)
### 응답 200
```json
{ "data": { "total": 12, "owned": 5,
            "items": [ { "species": { "id": "uuid", "code": "duck", "name": "오리", "rarity": "COMMON", "assetKey": "..." , "description": "..." },
                         "owned": true, "petId": "uuid", "obtainedAt": "..." },
                       { "species": { "...", "assetKey": "pets/cat/silhouette.png" }, "owned": false } ] } }
```
미보유 종은 `assetKey`를 실루엣으로 바꾸고 `description`을 숨긴다. `active=false` 종은 보유한 경우만 노출.

---

## P-8. DELETE `/api/v1/pets/{petId}` (M3)
### 응답 200 `data` 없음.
### 에러
| HTTP | code | 발생 조건 |
|---|---|---|
| 409 | PET_RELEASE_NOT_ALLOWED | 대표 펫이거나 마지막 1마리 |
### 부수효과
장착 아이템 전부 `reserved` 해제. 소프트 딜리트. 이후 같은 종을 다시 뽑으면 **새 펫**으로 지급(유니크 제약은 `deleted_at IS NULL`만 대상).
