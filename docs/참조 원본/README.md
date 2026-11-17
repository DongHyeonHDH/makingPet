# Project Blueprint — Spring Boot 모듈러 모놀리스 표준 양식

> **이 폴더의 용도**: 새 프로젝트를 시작할 때 다른 Claude 세션(또는 사람)이 그대로 참고해 **같은 구조·같은 문서 체계·같은 인프라**로 세팅할 수 있도록 만든 범용 템플릿입니다.
> 원본은 `Asset-Box`(Spring Boot 4 / Java 25 / MySQL / Redis / S3 / Docker Compose / GitHub Actions)이며, 도메인 고유 내용은 모두 걷어내고 `{{플레이스홀더}}`로 바꿨습니다.

---

## 다른 세션에게 주는 사용 지시 (복붙용)

```text
첨부한 _blueprint/ 폴더의 문서를 기준으로 새 프로젝트를 세팅해줘.
- 프로젝트 이름: {{PROJECT_NAME}}
- 베이스 패키지: {{BASE_PACKAGE}}   (예: io.myname.myapp)
- 도메인 목록: {{DOMAINS}}           (예: user, post, comment)
순서: 01 트리 생성 → 04 인프라/config 파일 생성 → 02 기획 문서 골격 생성 → 03 API 문서 골격 생성.
플레이스홀더({{...}})는 위 값으로 치환하고, 값이 없는 것은 TODO로 남겨줘.
```

---

## 파일 목록

| 파일 | 내용 | 언제 보나 |
|---|---|---|
| [`01_프로젝트_트리.md`](./01_프로젝트_트리.md) | 레포 최상위 트리, 도메인 패키지 5폴더 규칙, `common/` 구성, 테스트 트리, 의존성 규칙 | 프로젝트 생성 직후 |
| [`02_기획_문서_구조.md`](./02_기획_문서_구조.md) | `기획/` 폴더 00~08 체계와 각 문서의 섹션 양식(개요·마일스톤·ERD·기능 카탈로그·파트 가이드·협업 규칙) | 킥오프 / 설계 단계 |
| [`03_API_명세서_구조.md`](./03_API_명세서_구조.md) | API 표준(응답 래퍼·에러코드·인증·페이지네이션·멀티파트), 엔드포인트 목록 양식, 도메인 명세서 4-블록 양식, 에러 코드 사전 양식 + 대응 Java 코드 | API 설계 / 구현 |
| [`04_인프라_Config.md`](./04_인프라_Config.md) | `build.gradle`, `application*.yml`, `.env` 예시, Dockerfile, docker-compose, `.dockerignore`, `.gitignore`, GitHub Actions(CI/CD), `common/config` Java 설정 클래스, 배포 절차 | 인프라 세팅 / 배포 |

---

## 한눈에 보는 아키텍처 결정 (TL;DR)

1. **모듈러 모놀리스**: 하나의 Spring Boot 앱 안에서 도메인 패키지별로 경계를 나눈다. 다른 도메인은 `service`를 통해서만 호출한다.
2. **도메인 패키지마다 `controller / service / repository / domain / dto`** 다섯 폴더 고정.
3. **컨트랙트를 먼저 정하고 구현은 나중에**: `기획/04_api`와 `기획/03_erd`를 먼저 바꾸고 코드를 그 다음에 바꾼다.
4. **모든 응답은 `ApiResponse<T>`로 감싼다**, 에러는 `ErrorCode` enum과 `BusinessException`, 그리고 `GlobalExceptionHandler`로 처리한다.
5. **인증은 Stateless JWT**(Access와 Refresh, Refresh는 Redis에 저장)와 OAuth2 Client(소셜 로그인), Role 계층(`SUPER_ADMIN > ADMIN > USER`)으로 구성한다.
6. **인프라**: Docker 멀티스테이지 빌드 → GHCR → self-hosted runner에서 `docker compose pull && up` 실행. 앞단 리버스 프록시와 HTTPS는 Nginx Proxy Manager가 맡는다.
7. **브랜치**: `feature/* → dev(CI 테스트) → prod(검증) → main(태깅, 이미지 빌드, 배포)`.
