# 작업 기록: 2026-11-18 Docker Compose 로컬 인프라 (PostgreSQL + Redis)

> 범위: **로컬 개발용 인프라만** 다룬다. `app`·`frontend`·`nginx-proxy-manager` 컨테이너는 배포 단계에서 추가한다(참조 원본 04 §7).
> 앱은 호스트(IntelliJ 또는 `gradlew bootRun`)에서 실행하고 `localhost:5432`와 `localhost:6379`에 연결한다.

## 1. 추가한 파일

| 파일 | 내용 |
|---|---|
| `docker-compose.yml` | `db`(postgres:16-alpine), `redis`(redis:7-alpine, AOF, 비밀번호), 헬스체크, 볼륨 영속화, 포트는 `127.0.0.1`에만 바인딩 |
| `.env.example` | compose와 Spring이 함께 쓰는 환경변수 견본. `.env`로 복사해서 사용 |
| `.gitignore` | `.env`, `.env.*` 제외 (`.env.example`은 포함) |

`application.yaml`은 수정하지 않았다. 이미 `DB_URL / DB_USERNAME / DB_PASSWORD / REDIS_* / CACHE_TYPE`를 env로 받는다.

## 2. 실행 방법 (Windows, Docker Desktop)

```powershell
cd C:\ssafy\05_BE\makingPet
copy .env.example .env          # 처음 한 번만. 비밀번호는 원하면 바꾼다

docker compose up -d            # db, redis 기동
docker compose ps               # STATUS가 (healthy)인지 확인
```

### 접속 확인
```powershell
docker exec -it makingpet-db psql -U makingpet -d makingpet -c "select version();"
docker exec -it makingpet-redis redis-cli ping           # PONG
```

### 앱 실행
- **IntelliJ**: Run/Debug Configuration → `MakingPetApplication` → *Environment variables* 오른쪽 아이콘 → *Paths to ".env" files*에 `.env` 지정
  (버전이 낮아 이 옵션이 없으면 EnvFile 플러그인 사용)
- **PowerShell + Gradle**:
  ```powershell
  Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
      $k, $v = $_ -split '=', 2; [Environment]::SetEnvironmentVariable($k.Trim(), $v.Trim(), 'Process') }
  .\gradlew.bat bootRun
  ```
- 기동 시 Flyway가 `V1`, `V2`를 PostgreSQL에 적용하고 Hibernate는 `validate`만 한다.
- `.env` 없이 실행하면 기존처럼 H2 인메모리로 동작한다.

### 테스트를 PostgreSQL·Redis로 돌리기
위와 같이 env를 적용한 셸에서 `.\gradlew.bat test`를 실행한다. `CACHE_TYPE=redis`이면 `ItemCatalogRedisCacheTest`도 실행된다.

## 3. 자주 쓰는 명령

| 목적 | 명령 |
|---|---|
| 로그 보기 | `docker compose logs -f db` / `docker compose logs -f redis` |
| 중지 (데이터 유지) | `docker compose stop` 또는 `docker compose down` |
| **DB 초기화** (볼륨 삭제) | `docker compose down -v`, 그다음 `docker compose up -d` |
| Redis 키 확인 | `docker exec -it makingpet-redis redis-cli --scan --pattern "makingpet:*"` |

## 4. 주의 사항
- **포트 충돌**: PC에 PostgreSQL이 설치돼 있어 5432가 사용 중이면 `.env`에서 `POSTGRES_PORT=5433`과 `DB_URL=jdbc:postgresql://localhost:5433/makingpet`을 함께 바꾼다. Redis도 마찬가지다.
- **비밀번호 변경**: `POSTGRES_PASSWORD`는 **볼륨이 처음 만들어질 때만** 적용된다. 바꾸려면 `docker compose down -v`로 볼륨을 지운다.
- **기존 DB를 ddl-auto=update로 만든 경우**: 처음 한 번만 `FLYWAY_BASELINE_ON_MIGRATE=true`로 실행한다. 새 볼륨이면 필요 없다.
- 버전: 2026-11-17 검증은 PG14 + Redis 6에서 했다. 이 compose는 PG16 + Redis 7이다. Flyway 12는 두 버전 모두 지원한다.
- `04_게임_동시성_설계.md` §7의 Testcontainers 도입은 이 compose와 별개다(테스트가 컨테이너를 직접 띄운다).
