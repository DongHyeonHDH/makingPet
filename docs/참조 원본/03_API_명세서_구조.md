# 03. API 명세서 구조 (템플릿)

> `기획/04_api/` 구성: **표준(01) → 엔드포인트 목록(02, SSoT) → 도메인별 명세(03~) → 에러 코드 사전(마지막)**
> 충돌 시 우선순위: `02_엔드포인트_목록` > 도메인 명세서 > 코드.

---

## 0. `04_api/README.md` 양식

```markdown
# 04. API 문서 인덱스
## 읽는 순서
| 순서 | 파일 | 무엇 |
| 1 | 01_API_표준.md | 응답 포맷, 인증, 페이지네이션, 멀티파트 |
| 2 | 02_엔드포인트_목록.md | 전 엔드포인트 한 장 요약 (SSoT) |
| 3.. | 0N_명세서_{Domain}.md | 도메인 상세 |
| 끝 | NN_에러_코드_사전.md | 전 도메인 에러 코드 |
## 도메인 ↔ 명세서 ↔ 담당
## 변경 시 절차
1. 명세서 먼저 수정 → 2. 코드 PR → 3. 에러 코드 추가 시 사전+명세서 같은 PR
4. 필드 제거/이름 변경은 회의 안건, 추가는 자유
## 빠른 참조 (헤더 / 성공 / 실패 / 페이지 응답)
## 통계 | 도메인 | 엔드포인트 수 | 에러 코드 수 |
```

---

## 1. `01_API_표준.md` 양식 (내용 그대로 재사용 가능)

### 1-1) 공통 응답 포맷

**성공**
```json
{ "success": true, "message": "게시물이 정상적으로 생성되었습니다.", "data": { } }
```
**실패**
```json
{ "success": false, "message": "해당 요청이 실패되었습니다.",
  "error": { "code": "POST_NOT_FOUND", "message": "해당 게시글을 찾을 수 없습니다." } }
```
**검증 실패**: `error.code = VALIDATION_FAILED` + `error.fields[]`(권장)
```json
"error": { "code": "VALIDATION_FAILED", "message": "검증에 실패했습니다.",
           "fields": [ { "field": "title", "reason": "must not be blank" } ] }
```
**페이지네이션** — Spring `Page`를 그대로 직렬화하지 말고 `PageResponse.of(page, mapper)`로 변환한다.
```json
"data": { "items": [], "page": 0, "size": 20, "totalElements": 137,
          "totalPages": 7, "first": true, "last": false }
```
> 무한 스크롤이 필요하면 `Slice` 기반 `{ items, page, size, hasNext }` 형식도 허용하되, 도메인 안에서는 하나로 통일한다.

### 1-2) HTTP 상태 코드 매핑
| 상태 | 언제 | 예시 code |
|---|---|---|
| 200 | 일반 성공 | - |
| 201 | 생성 | - |
| 204 | 본문 없는 성공(선택) | - |
| 400 | 검증/비즈니스 입력 오류 | `VALIDATION_FAILED` |
| 401 | 토큰 없음/만료/위조, 로그인 실패 | `UNAUTHORIZED`, `TOKEN_EXPIRED`, `LOGIN_FAILED` |
| 403 | 권한 부족 | `FORBIDDEN` |
| 404 | 자원 없음 | `{DOMAIN}_NOT_FOUND` |
| 409 | 중복/충돌 | `{DOMAIN}_{FIELD}_DUPLICATED` |
| 500 | 그 외 | `INTERNAL_ERROR` |

### 1-3) 에러 코드 네이밍
`DOMAIN_SUBJECT_REASON` (UPPER_SNAKE). 예: `USER_EMAIL_DUPLICATED`, `FILE_EXTENSION_NOT_ALLOWED`, `REQUEST_STATUS_TRANSITION_INVALID`.

### 1-4) 인증
- 헤더: `Authorization: Bearer <AccessToken>`
- 발급: `POST /api/users/login`, 재발급: `POST /api/users/refresh` (Refresh 토큰은 Redis에 저장)
- JWT payload 예: `{ "sub": "12", "email": "...", "role": "USER", "iat": ..., "exp": ... }`
- 익명 허용: signup, login, oauth2 authorization, swagger(`/v3/**`), health
- WebSocket: STOMP `CONNECT` 헤더에 `Authorization: Bearer <jwt>`를 넣고 ChannelInterceptor에서 검증한다.

**권한 매트릭스**
| Role | 가능 |
|---|---|
| 익명 | 익명 허용 엔드포인트만 |
| USER | 일반 인증 엔드포인트 (`/api/admin/**` 불가) |
| ADMIN | USER + `/api/admin/**` |
| SUPER_ADMIN | ADMIN + 권한 변경 |

### 1-5) 페이지네이션 / 정렬 / 검색
| 파라미터 | 타입 | 기본 | 비고 |
|---|---|---|---|
| page | int | 0 | 0-based |
| size | int | 20 | 최대 50, 초과 시 `PAGINATION_SIZE_TOO_LARGE` |
| sort | string | 도메인별 | `createdAt,desc` 형식, **도메인별 화이트리스트**, 위반 시 `SORT_KEY_NOT_ALLOWED` |

검색 입구는 하나로 둔다: `GET /api/{resources}?q=&tag=&categoryId=&authorId=&sort=` (별도 `/search` 경로를 만들지 않는다).

### 1-6) 멱등성
`POST`는 비멱등이다(토글은 `POST /{id}/like` 하나로 처리). `PUT/PATCH/DELETE`는 멱등이다. mutation 응답에는 변경된 후의 상태를 담는다.

### 1-7) 엔드포인트 네이밍
- 명사 복수형 사용: `/api/posts`, `/api/users`
- 동사는 쓰지 않는다. 단, 액션성 서브리소스는 PATCH로 허용한다: `PATCH /api/requests/{id}/assign`
- 중첩은 1단계까지만: `/api/posts/{id}/comments`
- 관리자 API는 `/api/admin/{resources}/**` 아래에 둔다.

### 1-8) 멀티파트 업로드
| Part | 타입 | 비고 |
|---|---|---|
| `request` (또는 `data`) | application/json | `@RequestPart("request") @Valid XxxRequest` |
| `files` | binary | 0~N개, 합계 제한 명시 |
| `thumbnail` | image/png, image/jpeg | 0~1개 |

확장자 화이트리스트를 명시하고, 위반 시 `FILE_EXTENSION_NOT_ALLOWED`를 반환한다.

### 1-9) 날짜/시간
`LocalDateTime` → `"2026-05-21T14:30:15"`, `LocalDate` → `"2026-06-16"`. 서버 타임존을 명시한다(KST 또는 UTC 중 하나로 통일).

### 1-10) 헤더
`Content-Type: application/json` (또는 `multipart/form-data`), `Accept: application/json`, `Authorization: Bearer <jwt>`.

---

## 2. `02_엔드포인트_목록.md` 양식 (SSoT)

```markdown
# 02. 엔드포인트 목록
> 여기 없는 엔드포인트는 만들지 않는다. 추가가 필요하면 문서부터 갱신하고 그 다음 구현한다.
> 범례: 🔓 익명 · ✅ USER · 🛡 ADMIN · 👑 SUPER_ADMIN

## 0. 최근 결정 사항 (회의 반영 박스)
- 결정 1 / 결정 2 ...
- 상태 흐름이 있으면 다이어그램:  A(요청됨) → B(진행중) → C(완료)

## 1. {Domain}
| # | Method | Path | Auth | 요약 | 성공 | 주요 에러 |
|---|---|---|---|---|---|---|
| U-1 | POST | /api/users/signup | 🔓 | 회원가입 | 201 | 400 VALIDATION_FAILED, 409 USER_EMAIL_DUPLICATED |
| U-2 | POST | /api/users/login | 🔓 | 로그인 | 200 | 401 LOGIN_FAILED |
| U-3 | POST | /api/users/refresh | 🔓 | 토큰 재발급 | 200 | 401 REFRESH_TOKEN_EXPIRED |
| U-4 | GET  | /api/users/me | ✅ | 내 정보 | 200 | 401 |
...
## N. WebSocket
| 방향 | destination | payload | 설명 |
| SUB | /user/queue/messages | MessageResponse | 개인 DM 수신 |
```

---

## 3. 도메인 명세서 `0N_명세서_{Domain}.md` 양식 (4-블록 패턴)

~~~markdown
# 0N. API 명세서 — {Domain}
> 담당: {파트} · 베이스 경로: `/api/{resources}/**`, `/api/admin/{resources}/**`

| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| X-1 | POST | /api/{resources} | USER | 생성 |
| X-2 | GET | /api/{resources} | USER | 목록/검색 |
| X-3 | GET | /api/{resources}/{id} | USER | 상세 |
| X-4 | PUT | /api/{resources}/{id} | USER(작성자) | 수정 |
| X-5 | DELETE | /api/{resources}/{id} | USER(작성자) | 삭제(soft) |
| X-6 | GET | /api/admin/{resources} | ADMIN | 관리자 목록 |

---

## X-1. POST `/api/{resources}`

**설명**: 한 줄 요약
**인증**: 없음 / USER / ADMIN / SUPER_ADMIN

### 요청
```http
POST /api/{resources}
Content-Type: application/json
Authorization: Bearer <jwt>
```
```json
{ "title": "...", "content": "..." }
```
| 위치 | 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|---|
| body | title | string | Y | NotBlank, max 100 |
| query | page | int | N | 기본 0 |
| path | id | long | Y | |

### 응답 201
```json
{ "success": true, "message": "...", "data": { "id": 1, "title": "...", "createdAt": "2026-05-21T14:30:15" } }
```
| 필드 | 타입 | 설명 |
|---|---|---|

### 에러
| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | VALIDATION_FAILED | 필수값 누락 |
| 401 | UNAUTHORIZED | 토큰 없음 |
| 404 | {DOMAIN}_NOT_FOUND | id 미존재 |

### 부수효과 (있을 때만)
- 같은 트랜잭션에서 다른 도메인의 상태 변경, 알림 발송 등
~~~

---

## 4. 에러 코드 사전 `NN_에러_코드_사전.md` 양식

```markdown
# NN. 에러 코드 사전
## 0) 사용 규칙
1. 이 표에 없는 코드는 던지지 않는다 (먼저 추가)
2. 메시지는 가이드, 코드는 불변
3. 검증 실패는 VALIDATION_FAILED 하나로
4. 같은 의미를 다른 코드로 중복 정의 금지

## 1) 공통
| code | HTTP | 의미 / 발생 조건 |
| VALIDATION_FAILED | 400 | Bean Validation 실패 |
| PAGINATION_SIZE_TOO_LARGE | 400 | size > 50 |
| SORT_KEY_NOT_ALLOWED | 400 | |
| UNAUTHORIZED | 401 | 토큰 누락 |
| TOKEN_EXPIRED | 401 | |
| TOKEN_INVALID | 401 | |
| FORBIDDEN | 403 | |
| INTERNAL_ERROR | 500 | |

## 2) User · 인증
| USER_NOT_FOUND | 404 | |
| USER_EMAIL_DUPLICATED | 409 | |
| LOGIN_FAILED | 401 | 이메일/비번 구분하지 않음(보안) |
| REFRESH_TOKEN_EXPIRED | 401 | |
| FORBIDDEN_SELF_ROLE_CHANGE | 403 | |

## 3) {Domain} ...
## 범위 외(사용 금지) 코드
## ErrorCode enum 작성 패턴 (아래 5절 코드)
```

---

## 5. 문서와 1:1 대응되는 공통 코드 (그대로 복사 가능)

### `common/dto/ApiResponse.java`
```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, T data, ErrorBody error) {

    public static <T> ApiResponse<T> ok(T data, String successMessage) {
        return new ApiResponse<>(true, successMessage, data, null);
    }
    public static <T> ApiResponse<T> created(T data, String successMessage) {
        return ok(data, successMessage);
    }
    public static ApiResponse<Void> ok(String successMessage) {
        return ok(null, successMessage);
    }
    public static ApiResponse<Void> fail(String code, String errorMessage) {
        return new ApiResponse<>(false, "해당 요청이 실패되었습니다.", null, new ErrorBody(code, errorMessage));
    }
    public record ErrorBody(String code, String message) {}
}
```

### `common/dto/PageResponse.java` (권장 추가)
```java
public record PageResponse<T>(List<T> items, int page, int size,
                              long totalElements, int totalPages, boolean first, boolean last) {
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.map(mapper).getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
```

### `common/constants/ErrorCode.java`
```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // ===== Common =====
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "검증에 실패했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "현재 계정에 권한이 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),
    // ===== User =====
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USER_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 이메일입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "잘못된 비밀번호 또는 존재하지 않는 이메일입니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Refresh Token이 만료되었습니다."),
    // ===== {Domain} ===== (도메인별 섹션, append-only)
    ;
    private final HttpStatus status;
    private final String description;
}
```

### `common/constants/SuccessCode.java`
```java
@Getter
@RequiredArgsConstructor
public enum SuccessCode {
    // USER
    USER_CREATED("계정이 정상적으로 생성되었습니다."),
    USER_SIGNIN("계정에 성공적으로 로그인되었습니다."),
    // {DOMAIN}
    ;
    private final String successMessage;
}
```

### `common/exception/BusinessException.java` + `common/util/PreConditions.java`
```java
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    public BusinessException(ErrorCode errorCode) { super(errorCode.getDescription()); this.errorCode = errorCode; }
    public BusinessException(ErrorCode errorCode, String message) { super(message); this.errorCode = errorCode; }
    public ErrorCode getErrorCode() { return errorCode; }
}

public final class PreConditions {
    public static void validate(boolean expression, ErrorCode errorCode) {
        if (!expression) throw new BusinessException(errorCode);
    }
}
```

### `common/eventhandler/GlobalExceptionHandler.java`
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.fail(code.name(), e.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.VALIDATION_FAILED.name(), ErrorCode.VALIDATION_FAILED.getDescription()));
    }
    // 권장: Exception.class → 500 INTERNAL_ERROR (스택트레이스 비노출, 로그만)
}
```

### 컨트롤러 작성 패턴
```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/{resources}")
public class {Domain}Controller {
    private final {Domain}Service service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<{Domain}Response> create(
            @Valid @RequestPart("request") {Domain}CreateRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.created(service.create(currentUser, request, thumbnail),
                SuccessCode.{DOMAIN}_CREATED.getSuccessMessage());
    }

    @GetMapping
    public ApiResponse<PageResponse<{Domain}SummaryResponse>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(service.list(pageable), SuccessCode.{DOMAIN}_READ.getSuccessMessage());
    }
}
```

### Swagger(springdoc)
`/v3/api-docs`와 `/v3/swagger-ui`를 사용하고, 도메인마다 `GroupedOpenApi`를 하나씩 등록한다. Bearer JWT SecurityScheme을 전역으로 적용한다(구성은 `04_인프라_Config.md` 참고).
