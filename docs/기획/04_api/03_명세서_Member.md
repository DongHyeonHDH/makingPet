# 03. API 명세서 — Member (Auth)

> 베이스 경로: `/api/v1/auth/**`, `/api/v1/members/**` · 패키지: `member` (현재 `auth`, 결정 D-6에 따라 개명)
> 현재 구현된 M-1, M-2, M-5, M-6은 **응답을 `ApiResponse<T>` 래퍼로 바꾸고** 아래 필드를 추가한다.

| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| M-1 | POST | /api/v1/auth/register | 🔓 | 회원가입 |
| M-2 | POST | /api/v1/auth/login | 🔓 | 로그인 |
| M-3 | POST | /api/v1/auth/refresh | 🔓 (M2) | 토큰 재발급 |
| M-4 | POST | /api/v1/auth/logout | ✅ (M2) | 로그아웃 |
| M-5 | GET | /api/v1/members/me | ✅ | 내 정보 |
| M-6 | PATCH | /api/v1/members/me | ✅ | 닉네임 변경 |
| M-7 | DELETE | /api/v1/members/me | ✅ (M3) | 탈퇴 |

---

## M-1. POST `/api/v1/auth/register`
**설명**: 회원가입. 같은 트랜잭션에서 지갑(0코인) 생성 → 가입 선물 300코인 입금(원장 `SIGNUP_GIFT`) → 기본 테마 아이템 1개 지급 → 마이 홈 생성.
**인증**: 없음

### 요청
```json
{ "email": "duck@example.com", "password": "hunter2hunter2", "nickname": "오리사랑" }
```
| 위치 | 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|---|
| body | email | string | Y | Email, max 254, 소문자 정규화 |
| body | password | string | Y | 8~128 |
| body | nickname | string | Y | 1~24, 앞뒤 공백 제거, 금칙어 검사 |

### 응답 201
```json
{ "success": true, "message": "가입을 환영합니다.",
  "data": { "id": "uuid", "email": "duck@example.com", "nickname": "오리사랑", "role": "USER",
            "wallet": { "balance": 300 }, "createdAt": "2026-11-16T23:30:00+09:00" } }
```
### 에러
| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | VALIDATION_FAILED | 형식 오류 |
| 400 | MEMBER_NICKNAME_FORBIDDEN_WORD | 금칙어 |
| 409 | MEMBER_EMAIL_DUPLICATED | 이메일 중복 |
| 409 | MEMBER_NICKNAME_DUPLICATED | 닉네임 중복 |

### 부수효과
- wallet.credit(300, SIGNUP_GIFT), item.grant(기본 테마), home 생성.

---

## M-2. POST `/api/v1/auth/login`
**인증**: 없음
### 요청
```json
{ "email": "duck@example.com", "password": "hunter2hunter2" }
```
### 응답 200
```json
{ "success": true, "message": "로그인되었습니다.",
  "data": { "accessToken": "eyJ...", "tokenType": "Bearer", "expiresIn": 1800,
            "refreshToken": "opaque-uuid", "refreshExpiresIn": 604800 } }
```
`refreshToken`, `refreshExpiresIn`은 M2부터. M1에서는 생략(NON_NULL).
### 에러
| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | INVALID_CREDENTIALS | 이메일/비밀번호 불일치 (구분하지 않음) |

---

## M-3. POST `/api/v1/auth/refresh` (M2)
**인증**: 없음 (Refresh 토큰 자체가 자격)
### 요청
```json
{ "refreshToken": "opaque-uuid" }
```
### 응답 200
M-2와 같은 body. Refresh 토큰은 **회전(rotation)** 한다: 이전 토큰은 즉시 폐기.
### 에러
| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | REFRESH_TOKEN_INVALID | 없음·만료·이미 사용됨 |

---

## M-4. POST `/api/v1/auth/logout` (M2)
**인증**: USER. body 없음. 해당 member의 Refresh 토큰을 모두 폐기한다. Access 토큰은 만료까지 유효(짧으므로 허용).
### 응답 200 `data` 없음.

---

## M-5. GET `/api/v1/members/me`
**인증**: USER
### 응답 200
```json
{ "success": true, "message": "조회되었습니다.",
  "data": { "id": "uuid", "email": "duck@example.com", "nickname": "오리사랑", "role": "USER",
            "wallet": { "balance": 1230 },
            "representativePet": { "...PetSummary 또는 null" },
            "today": { "attendanceClaimed": true, "rpsClaimed": false, "slotRemaining": 3 },
            "petCount": 3, "createdAt": "..." } }
```
| 필드 | 설명 |
|---|---|
| representativePet | 없으면 null → 클라이언트는 뽑기 화면으로 안내 |
| today | R-1과 같은 값. 상단 배지용 |

---

## M-6. PATCH `/api/v1/members/me`
### 요청
```json
{ "nickname": "오리킹" }
```
### 응답 200: M-5와 같은 body.
### 에러
| HTTP | code | 발생 조건 |
|---|---|---|
| 409 | MEMBER_NICKNAME_DUPLICATED | |
| 400 | MEMBER_NICKNAME_FORBIDDEN_WORD | |

---

## M-7. DELETE `/api/v1/members/me` (M3)
### 요청
```json
{ "password": "hunter2hunter2" }
```
### 응답 200 `data` 없음.
### 부수효과
- `members.deleted_at` 설정, 이메일·닉네임을 `deleted_<uuid>`로 치환(재가입 허용).
- ON_SALE 매물 전부 CANCELLED 처리(아이템은 반환되지만 계정이 닫히므로 의미 없음, 기록용).
- Refresh 토큰 폐기.
### 에러
| 401 | INVALID_CREDENTIALS | 비밀번호 불일치 |
