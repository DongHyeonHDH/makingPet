# 04. API 문서 인덱스

> ⚠️ 2026-11-16 API 명세서(01~10, 12)와 프로토콜 명세서(11) 초안 작성 완료. 엔드포인트 68개(+v2 companion 4), 에러 코드 55개.
> 충돌 시 우선순위: `02_엔드포인트_목록` > 도메인 명세서 > 코드.

## 읽는 순서
| 순서 | 파일 | 무엇 |
|---|---|---|
| 1 | 01_API_표준.md | 응답 래퍼, 상태 코드, 인증, 페이지네이션, 멱등 키, 공통 응답 조각 |
| 2 | 02_엔드포인트_목록.md | 전 엔드포인트 65개 한 장 요약 (SSoT) |
| 3 | 03_명세서_Member.md | 가입·로그인·토큰·내 정보 |
| 4 | 04_명세서_Pet.md | 펫 목록·상호작용·장착·도감 |
| 5 | 05_명세서_Gacha.md | 확률표·뽑기(1/10연, 천장, 중복 환급)·기록 |
| 6 | 06_명세서_Item_Home.md | 카탈로그·인벤토리·마이 홈·방문 |
| 7 | 07_명세서_Wallet_Reward.md | 잔액·원장·출석·가위바위보·슬롯 |
| 8 | 08_명세서_Shop.md | 상점 |
| 9 | 09_명세서_Auction.md | 경매장 등록·검색·구매·취소·찜·시세·만료 |
| 10 | 10_명세서_Community_Admin.md | 공지, 관리자 15개 |
| 11 | 11_프로토콜_명세서.md | SSE 알림 스트림(M3), 클라이언트 연출 규약(motionKey·뽑기·슬롯), 에셋 경로·캐시, v2 AI 펫 클라이언트 통신 |
| 끝 | 12_에러_코드_사전.md | 55개 코드 |

## 도메인 ↔ 명세서 ↔ 패키지
| 도메인 | 명세서 | 패키지 | 마일스톤 |
|---|---|---|---|
| member | 03 | `member` (현 `auth` 개명) | M1 |
| pet | 04 | `pet` | M1 |
| gacha | 05 | `gacha` | M1 |
| item, home | 06 | `item`, `home` | M1 / M2 |
| wallet, reward | 07 | `wallet`, `reward` | M1 |
| shop | 08 | `shop` | M1 |
| auction | 09 | `auction` | M2 |
| community, admin | 10 | `community`, `admin` | M1 (화면 M3) |
| notification | 11 | `notification` | M3 |
| companion | 11 §4 | `companion` | v2 |

## 변경 시 절차
1. 명세서 먼저 수정 (`02` 목록 → 도메인 문서) → 2. 코드 PR → 3. 에러 코드 추가 시 `12` 사전 + `ErrorCode` enum을 같은 PR에서.
4. 필드 제거·이름 변경은 변경 박스에 기록. 추가는 자유.

## 빠른 참조
```http
Authorization: Bearer <jwt>
Idempotency-Key: <uuid>        # 뽑기·슬롯·상점 구매·경매 등록/구매
```
```json
{ "success": true,  "message": "...", "data": { } }
{ "success": false, "message": "해당 요청이 실패되었습니다.", "error": { "code": "...", "message": "...", "fields": [ ] } }
{ "data": { "items": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0, "first": true, "last": true } }
```

## 확정된 결정
| # | 결정 | 확정값 |
|---|---|---|
| D-1 | 경로 접두사 | `/api/v1` |
| D-2 | 응답 형식 | `ApiResponse<T>` 래퍼 (기존 AuthController·SecurityConfig 수정) |
| D-3 | 외부 ID | UUID |
| D-4 | DB | PostgreSQL |
| D-5 | Refresh 토큰 | M2, Redis, 회전 방식 |
| D-6 | `auth` 패키지 | `member`로 개명 + 5폴더 규칙 |
| D-7 | 멱등 키 | 코인·아이템이 움직이는 POST 5종에 필수 |
| D-8 | 중복 펫 | 코인 환급 (종당 1마리) |
| D-9 | 실시간 | M3 SSE 1채널 (프로토콜 명세서에서 상세) |
| D-10 | 쿨다운 HTTP | 429 + `Retry-After` |
| D-11 | 남의 마이 홈 | 익명 허용 (PUBLIC만) |
| D-12 | 신발 | ACCESSORY 슬롯 |

## 기존 코드에 필요한 변경 (M0 작업)
| 파일 | 변경 |
|---|---|
| `AuthController` | 응답을 `ApiResponse<T>`로 래핑. 에러 코드 `EMAIL_IN_USE→MEMBER_EMAIL_DUPLICATED`, `NICKNAME_IN_USE→MEMBER_NICKNAME_DUPLICATED`, `INVALID_REQUEST→VALIDATION_FAILED`. 로컬 `@ExceptionHandler` 제거 → `GlobalExceptionHandler` |
| `SecurityConfig.writeError` | `{code,message}` → `ApiResponse.fail(...)` 형식 |
| `SecurityConfig` 허용 경로 | `02` 목록의 🔓와 일치시키기: `/gacha/pools`, `/items`, `/homes/{nickname}`, `/notices/**`, `/auction/prices` 추가. `/community/posts/**`는 C-2 미도입이므로 제거 |
| `MemberAccount` | `role`, `representative_pet_id` 추가, `BaseEntity` 상속 |
| `common/` | `ApiResponse`, `PageResponse`, `ErrorCode`, `BusinessException`, `GlobalExceptionHandler`, `BaseEntity` 생성 (참조 원본 03 §5 코드 사용) |

## 통계
| 도메인 | 엔드포인트 | 에러 코드 |
|---|---|---|
| Member 7 · Pet 8 · Gacha 4 · Item/Inv 2 · Home 6 · Wallet 2 · Reward 5 · Shop 3 · Auction 11 · Community 2 · Admin 15 · Notification 3 | **68** (+v2 4) | **55** |
