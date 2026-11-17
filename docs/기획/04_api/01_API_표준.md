# 01. API 표준

> ⚠️ 2026-11-16 확정. 본 박스가 최신이며, 본문과 충돌하면 박스를 우선한다.
> 결정 반영: D-1 `/api/v1`, D-2 `ApiResponse<T>` 래퍼, D-3 UUID, D-7 멱등 키, D-10 쿨다운 429.

## 1. 공통 응답 포맷 (`ApiResponse<T>`)

**성공**
```json
{ "success": true, "message": "펫을 뽑았습니다.", "data": { } }
```
**실패**
```json
{ "success": false, "message": "해당 요청이 실패되었습니다.",
  "error": { "code": "WALLET_INSUFFICIENT_BALANCE", "message": "코인이 부족합니다." } }
```
**검증 실패**
```json
{ "success": false, "message": "해당 요청이 실패되었습니다.",
  "error": { "code": "VALIDATION_FAILED", "message": "검증에 실패했습니다.",
             "fields": [ { "field": "nickname", "reason": "size must be between 1 and 24" } ] } }
```
- `data`와 `error`는 null이면 직렬화하지 않는다(`@JsonInclude(NON_NULL)`).
- `message`는 사람이 읽는 안내문이고 바뀔 수 있다. 클라이언트 분기는 **`error.code`로만** 한다.
- Spring Security가 직접 쓰는 401/403 응답(`SecurityConfig.writeError`)도 같은 형식이어야 한다. → 기존 `{code,message}` 출력 수정.

**페이지네이션** — `PageResponse<T>`
```json
"data": { "items": [], "page": 0, "size": 20, "totalElements": 137,
          "totalPages": 7, "first": true, "last": false }
```

## 2. HTTP 상태 코드 매핑
| 상태 | 언제 | 예시 code |
|---|---|---|
| 200 | 조회·변경 성공 | |
| 201 | 생성 (가입, 매물 등록, 뽑기 결과 생성) | |
| 400 | 검증 실패, 비즈니스 입력 오류, 잔액·수량 부족 | `VALIDATION_FAILED`, `WALLET_INSUFFICIENT_BALANCE` |
| 401 | 토큰 없음/만료/위조, 로그인 실패 | `TOKEN_REQUIRED`, `TOKEN_EXPIRED`, `INVALID_TOKEN`, `INVALID_CREDENTIALS` |
| 403 | 권한 부족, 남의 자원 | `FORBIDDEN`, `PET_NOT_OWNED` |
| 404 | 자원 없음 | `{DOMAIN}_NOT_FOUND` |
| 409 | 중복·상태 충돌 (이미 참여, 이미 판매됨, 닉네임 중복) | `REWARD_ALREADY_CLAIMED`, `AUCTION_LISTING_NOT_ON_SALE` |
| 429 | 쿨다운·일일 한도 초과 (`Retry-After` 헤더 동반) | `PET_INTERACTION_COOLDOWN`, `REWARD_DAILY_LIMIT_EXCEEDED` |
| 500 | 그 외 | `INTERNAL_ERROR` |

## 3. 에러 코드 네이밍
`DOMAIN_SUBJECT_REASON` (UPPER_SNAKE). 전체 목록은 `12_에러_코드_사전.md`. **사전에 없는 코드는 던지지 않는다.**

## 4. 인증
- 헤더: `Authorization: Bearer <AccessToken>`
- Access Token: HS256 JWT, 30분. payload 예
  ```json
  { "iss": "makingpet", "sub": "<member uuid>", "role": "USER", "iat": 1731760000, "exp": 1731761800 }
  ```
  `role` 클레임은 M1에서 추가한다(현재 코드에는 없음).
- Refresh Token (M2): `POST /api/v1/auth/refresh`, Redis 저장, 7일. 응답 body로 전달하고 클라이언트가 보관한다.
- 익명 허용 엔드포인트는 `02_엔드포인트_목록.md`에 🔓로 표시한다. 그 외 `/api/v1/**`는 인증 필수, `/api/v1/admin/**`는 ADMIN 이상.

**권한 매트릭스**
| Role | 가능 |
|---|---|
| 익명 | 🔓 엔드포인트 (가입·로그인·공지·상점/경매 조회·공개 마이 홈·뽑기 확률표) |
| USER | 일반 인증 엔드포인트 |
| ADMIN | USER + `/api/v1/admin/**` (카탈로그·뽑기 풀·공지·매물 내림) |
| SUPER_ADMIN | ADMIN + 재화 보정, 권한 변경 |

**자원 소유 검사**: 내 펫·내 매물처럼 소유자가 있는 자원에 남이 접근하면 403 `{DOMAIN}_NOT_OWNED`. 존재 자체를 숨길 필요는 없다(ID가 UUID라 열거가 어렵다).

## 5. 식별자
- 모든 외부 노출 ID는 **UUID 문자열**.
- 카탈로그(펫 종·아이템)는 ID와 함께 `code`(`duck`, `ribbon`)도 돌려준다. 클라이언트 에셋 매핑에 쓴다.

## 6. 페이지네이션 / 정렬 / 검색
| 파라미터 | 타입 | 기본 | 비고 |
|---|---|---|---|
| page | int | 0 | 0-based |
| size | int | 20 | 최대 50, 초과 시 400 `PAGINATION_SIZE_TOO_LARGE` |
| sort | string | 도메인별 | `price,asc` 형식, 도메인별 화이트리스트, 위반 시 400 `SORT_KEY_NOT_ALLOWED` |

검색 입구는 하나: `GET /api/v1/auction/listings?q=&category=&maxPrice=&sort=`. `/search` 경로는 만들지 않는다.

## 7. 멱등성 (D-7)
- `PUT / PATCH / DELETE`는 멱등. `POST`는 비멱등이 기본.
- **코인이나 아이템이 움직이는 POST**(뽑기, 슬롯, 상점 구매, 경매 구매, 매물 등록)는 `Idempotency-Key: <uuid>` 헤더를 **필수**로 받는다.
  - 같은 (member, key)로 24시간 안에 다시 오면 **최초 응답을 그대로** 돌려준다(처리하지 않음).
  - 같은 key에 다른 body가 오면 409 `IDEMPOTENCY_KEY_REUSED`.
  - 헤더가 없으면 400 `IDEMPOTENCY_KEY_REQUIRED`.
  - 저장소: M1은 DB 테이블 `idempotency_keys(member_id, key, request_hash, response_json, created_at)`, M2에 Redis TTL로 옮길 수 있다.
- 출석·가위바위보는 "하루 1회" 유니크 제약이 이미 멱등을 보장하므로 헤더를 받지 않는다.
- mutation 응답에는 **변경된 뒤의 상태**(예: 새 잔액)를 담는다.

## 8. 엔드포인트 네이밍
- 명사 복수형: `/pets`, `/listings`. 동사 금지. 액션성 서브리소스는 POST/PATCH 허용: `POST /pets/{id}/interactions`, `PATCH /pets/{id}/representative`.
- 중첩은 1단계까지.
- 내 것 조회는 `/me` 또는 소유자 암묵 자원(`/pets`, `/inventory`, `/wallet`, `/home`)으로 둔다. 남의 자원은 `/homes/{nickname}`처럼 별도 복수형 경로.
- 관리자: `/api/v1/admin/{resources}/**`.

## 9. 날짜/시간
- 모든 시각은 **ISO-8601 with offset**: `"2026-11-16T23:30:00+09:00"`. 서버 타임존 `Asia/Seoul`.
- 날짜만 쓰는 값(출석일)은 `"2026-11-16"`.
- "하루"의 경계는 KST 00:00.

## 10. 헤더
| 헤더 | 값 |
|---|---|
| Content-Type | `application/json` |
| Accept | `application/json` |
| Authorization | `Bearer <jwt>` |
| Idempotency-Key | UUID (7절 대상 POST만) |
| Retry-After | 429 응답에 초 단위 |

## 11. 공통 응답 조각 (여러 명세서에서 재사용)
```json
// PetSummary
{ "id": "uuid", "name": "꽥꽥이", "species": { "id": "uuid", "code": "duck", "name": "오리", "rarity": "COMMON", "assetKey": "pets/duck/base.png" },
  "mood": 72, "moodState": "HAPPY", "affectionLevel": 2, "isRepresentative": true,
  "equipment": { "TOP": { "itemId": "uuid", "code": "cloud_tee", "assetKey": "items/cloud_tee.png" }, "BOTTOM": null, "ACCESSORY_1": null, "ACCESSORY_2": null, "MOTION": { "itemId": "uuid", "code": "motion.idle", "assetKey": "motion.idle" } } }

// ItemSummary
{ "id": "uuid", "code": "ribbon", "name": "딸기 리본", "category": "ACCESSORY", "rarity": "COMMON", "assetKey": "items/ribbon.png", "tradable": true }

// WalletSnapshot (코인이 움직인 응답마다 포함)
{ "balance": 1230 }

// MemberPublic (남에게 보이는 정보)
{ "id": "uuid", "nickname": "오리사랑" }
```
`moodState`: `SAD`(0~29) / `NORMAL`(30~69) / `HAPPY`(70~100).
