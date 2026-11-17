# 04. 인프라 · Config 설정 (템플릿)

> 스택: **Spring Boot 4.0.x · Java 25 · Gradle · MySQL 8 · Redis 7 · AWS S3 · Docker Compose · GHCR · GitHub Actions(self-hosted 배포) · Nginx Proxy Manager(HTTPS)**
> 플레이스홀더: `{{PROJECT_NAME}}`, `{{GROUP}}`(예: `io.myname`), `{{GH_OWNER}}/{{GH_REPO}}`, `{{DOMAIN_NAME}}`, `{{FRONT_REPO}}`.
> 모든 시크릿은 **환경변수로 주입**하고 레포에는 `*.example` 파일만 둔다.

---

## 1. 배포 토폴로지

```
            Internet
               │ 80/443
   ┌───────────▼────────────┐   81(관리 UI, 관리자 IP만 허용)
   │  nginx-proxy-manager   │  Let's Encrypt 인증서 발급
   └───────────┬────────────┘
               │ http://frontend:80
   ┌───────────▼────────────┐
   │  frontend (nginx+SPA)  │  /api → app:8080 프록시
   └───────────┬────────────┘
               │
   ┌───────────▼────────────┐
   │  app (Spring Boot)     │  :8080 (외부 미노출)
   └──────┬──────────┬──────┘
          │          │
   ┌──────▼───┐ ┌────▼─────┐       ┌──────────┐
   │ db MySQL │ │  redis   │       │  AWS S3  │ (외부)
   └──────────┘ └──────────┘       └──────────┘
        docker network: {{PROJECT_NAME}}  (bridge)
```

**CI/CD 흐름**
```
feature/* ─PR→ dev  : ci-test.yml   (Redis 서비스 컨테이너 + ./gradlew test, profile=test)
prod      ─PR→ main : ci-cd.yml
   main push → ① 태깅 + GitHub Release (github-tag-action)
             → ② bootJar 빌드 → Docker 이미지 → GHCR push (tag: vX.Y.Z, latest)
             → ③ self-hosted runner(EC2) → docker compose pull && up -d --no-build
```

---

## 2. `settings.gradle` / `build.gradle`

```groovy
// settings.gradle
rootProject.name = '{{PROJECT_NAME}}'
```

```groovy
// build.gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.6'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = '{{GROUP}}'
version = '0.0.1-SNAPSHOT'
description = '{{PROJECT_NAME}}'

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
}

repositories { mavenCentral() }

dependencies {
    // Web / Security / Validation / JPA
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'

    // QueryDSL (OpenFeign fork)
    implementation 'io.github.openfeign.querydsl:querydsl-core:7.1'
    implementation 'io.github.openfeign.querydsl:querydsl-jpa:7.1'
    annotationProcessor 'io.github.openfeign.querydsl:querydsl-apt:7.1:jakarta'
    annotationProcessor 'jakarta.annotation:jakarta.annotation-api'
    annotationProcessor 'jakarta.persistence:jakarta.persistence-api'

    // DB / Migration
    runtimeOnly 'com.mysql:mysql-connector-j'
    runtimeOnly 'com.h2database:h2'                       // 테스트/로컬
    implementation 'org.springframework.boot:spring-boot-h2console'
    implementation 'org.springframework.boot:spring-boot-starter-flyway'
    implementation 'org.flywaydb:flyway-mysql'

    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.13.0'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.13.0'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.13.0'

    // Redis (+ 분산락)
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.redisson:redisson-spring-boot-starter:4.4.0'

    // S3
    implementation 'software.amazon.awssdk:s3:2.45.1'
    implementation 'io.awspring.cloud:spring-cloud-aws-starter-s3:4.0.2'

    // API 문서
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2'

    // (선택) 모니터링
    // implementation 'org.springframework.boot:spring-boot-starter-actuator'
    // runtimeOnly 'io.micrometer:micrometer-registry-prometheus'

    // (선택) 알림
    // implementation 'com.slack.api:slack-api-client:1.43.1'

    // Lombok / DevTools
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    developmentOnly 'org.springframework.boot:spring-boot-devtools'

    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-security-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
    testCompileOnly 'org.projectlombok:lombok'
    testAnnotationProcessor 'org.projectlombok:lombok'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') { useJUnitPlatform() }
```

---

## 3. `src/main/resources/application.yml` (기본, 값은 모두 env)

```yaml
spring:
  application:
    name: {{PROJECT_NAME}}
  config:
    import: optional:file:.env.development[.properties]   # 로컬 개발 시 .env.development 자동 로드
  datasource:
    url: ${dbUrl}
    username: ${dbUserName}
    password: ${dbPassWord}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
  jpa:
    hibernate:
      ddl-auto: ${DDL_AUTO:validate}    # 로컬: create-drop/update, 운영: validate (+Flyway)
    show-sql: false
    properties:
      hibernate:
        format_sql: true
  flyway:
    enabled: ${FLYWAY_ENABLED:false}
    baseline-on-migrate: false
    validate-on-migrate: true
  security:
    oauth2:
      client:
        registration:
          google:
            client-name: Google
            client-id: ${googleClientId}
            client-secret: ${googleClientSecretKey}
            redirect-uri: "{baseUrl}/{action}/oauth2/code/{registrationId}"
            scope: [email]
          naver:
            client-name: Naver
            client-id: ${naverClientId}
            client-secret: ${naverClientSecretKey}
            authorization-grant-type: authorization_code
            client-authentication-method: client_secret_post
            redirect-uri: "{baseUrl}/{action}/oauth2/code/{registrationId}"
            scope: [email]
        provider:
          naver:
            authorization-uri: https://nid.naver.com/oauth2.0/authorize
            token-uri: https://nid.naver.com/oauth2.0/token
            user-info-uri: https://openapi.naver.com/v1/nid/me
            user-name-attribute: response
  cloud:
    aws:
      region:
        static: ${s3Region}
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

custom:
  baseUrl: ${baseUrl}
  jwt:
    payload:
      issuer: ${JWT_ISSUER:{{PROJECT_NAME}}}
      subjectAccessToken: access-token
      subjectRefreshToken: refresh-token
      audiance: ${JWT_AUDIENCE:{{PROJECT_NAME}}}
    secrets:
      appkey: ${base64UuidKey}
      vanillakey: ${vanillaKey}
    validations:
      access: ${accessTokenTime}      # ms (예: 1800000 = 30분)
      refresh: ${refreshTokenTime}    # ms (예: 604800000 = 7일)
  s3:
    bucket-name: ${s3BucketName}
    access-key: ${s3AccessKey}
    secret-key: ${s3SecretKey}
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}

springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /v3/swagger-ui
    operations-sorter: alpha
    display-request-duration: true
    doc-expansion: none
    persist-authorization: true
```

## 4. `src/main/resources/application-test.yml`

```yaml
spring:
  h2:
    console:
      enabled: true
  datasource:
    url: jdbc:h2:mem:{{PROJECT_NAME}}-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  security:
    oauth2:
      client:
        registration:
          google: { client-id: test-google-client-id, client-secret: test-google-client-secret }
          naver:  { client-id: test-naver-client-id,  client-secret: test-naver-client-secret }
  cloud:
    aws:
      region:
        static: ap-northeast-2

custom:
  baseUrl: http://localhost:8080
  jwt:
    payload:
      issuer: test@{{PROJECT_NAME}}.local
      subjectAccessToken: access-token
      subjectRefreshToken: refresh-token
      audiance: {{PROJECT_NAME}}-test
    secrets:
      appkey: test-secret-key-for-jwt-signature-minimum-32-bytes
      vanillakey: test-vanilla-key
    validations:
      access: 3600000
      refresh: 1209600000
  s3:
    bucket-name: {{PROJECT_NAME}}-test
    access-key: test-access-key
    secret-key: test-secret-key
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
```

> 테스트는 `SPRING_PROFILES_ACTIVE=test`로 실행하고, Redis는 CI 서비스 컨테이너나 로컬 docker로 띄운다.

---

## 5. `.env.production.example` (레포에는 이 견본만 올린다)

```dotenv
# Copy to .env.production and replace every CHANGE_ME. (.env.* 는 gitignore)

# Database
MYSQL_DATABASE={{PROJECT_NAME}}
MYSQL_ROOT_PASSWORD=CHANGE_ME
dbUrl=jdbc:mysql://db:3306/{{PROJECT_NAME}}?serverTimezone=UTC&characterEncoding=UTF-8
dbUserName=root
dbPassWord=CHANGE_ME
DDL_AUTO=validate
FLYWAY_ENABLED=true

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# Service origin (CORS / OAuth redirect 기준)
baseUrl=https://{{DOMAIN_NAME}}

# JWT
base64UuidKey=CHANGE_ME_32_bytes_or_longer_secret
vanillaKey=CHANGE_ME_32_bytes_or_longer_secret
accessTokenTime=1800000
refreshTokenTime=604800000

# OAuth
googleClientId=CHANGE_ME
googleClientSecretKey=CHANGE_ME
naverClientId=CHANGE_ME
naverClientSecretKey=CHANGE_ME

# AWS S3
s3BucketName=CHANGE_ME
s3AccessKey=CHANGE_ME
s3SecretKey=CHANGE_ME
s3Region=ap-northeast-2

# Images (docker compose pull/up)
APP_IMAGE=ghcr.io/{{GH_OWNER}}/{{GH_REPO}}:latest
FRONT_IMAGE=ghcr.io/{{GH_OWNER}}/{{GH_REPO}}-front:latest
```

로컬 개발용 `.env.development`도 같은 키로 만들고(gitignore 대상), `dbUrl=jdbc:mysql://localhost:3306/...`, `DDL_AUTO=update` 정도로 둔다.

---

## 6. `Dockerfile` (멀티스테이지, non-root)

```dockerfile
# syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true   # 의존성 레이어 캐시

COPY src src
RUN ./gradlew --no-daemon clean bootJar -x test

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
RUN useradd -r -u 1001 appuser
COPY --from=builder /workspace/build/libs/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

## 7. `docker-compose.yml`

```yaml
name: {{PROJECT_NAME}}

services:
  db:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_CHARACTER_SET_SERVER: utf8mb4
      MYSQL_COLLATION_SERVER: utf8mb4_unicode_ci
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h 127.0.0.1 -uroot -p\"$$MYSQL_ROOT_PASSWORD\" || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s
    restart: unless-stopped
    networks: [app-net]

  redis:
    image: redis:7-alpine
    command: ["redis-server", "--appendonly", "yes"]
    volumes:
      - redis_data:/data
    restart: unless-stopped
    networks: [app-net]

  app:
    build: .
    image: ${APP_IMAGE:-ghcr.io/{{GH_OWNER}}/{{GH_REPO}}:latest}
    env_file:
      - .env.production
    environment:
      JAVA_TOOL_OPTIONS: "-Xms256m -Xmx512m"
    depends_on:
      db:    { condition: service_healthy }
      redis: { condition: service_started }
    restart: unless-stopped
    networks: [app-net]

  frontend:
    build:
      context: ../{{FRONT_REPO}}
      args:
        VITE_API_BASE_URL: /api
    image: ${FRONT_IMAGE:-ghcr.io/{{GH_OWNER}}/{{GH_REPO}}-front:latest}
    depends_on: [app]
    expose: ["80"]
    restart: unless-stopped
    networks: [app-net]

  nginx-proxy-manager:
    image: jc21/nginx-proxy-manager:latest
    depends_on: [frontend, app]
    ports:
      - "80:80"
      - "443:443"
      - "81:81"
    volumes:
      - npm_data:/data
      - npm_letsencrypt:/etc/letsencrypt
    restart: unless-stopped
    networks: [app-net]

volumes:
  mysql_data:
  redis_data:
  npm_data:
  npm_letsencrypt:

networks:
  app-net:
    driver: bridge
```

> 로컬에서 DB와 Redis만 띄울 때: `docker compose up -d db redis`를 실행한다. 이때는 db 서비스에 `ports: ["3306:3306"]`을 임시로 추가하거나 `docker-compose.local.yml`을 따로 만든다.

## 8. `.dockerignore`

```gitignore
.git
.gitignore
.github
build/
.gradle/
.idea/
*.iml
.env
.env.*
data/
uploads/
*.log
.DS_Store
README.md
DEPLOY.md
기획/
docs/
```

## 9. `.gitignore` (Spring 기본 + 추가)

```gitignore
HELP.md
.gradle
build/
!gradle/wrapper/gradle-wrapper.jar
!**/src/main/**/build/
!**/src/test/**/build/

### 환경파일 ###
.env.development
.env.production

### IDE ###
.idea
*.iws
*.iml
*.ipr
out/
.vscode/
.classpath
.project
.settings
bin/

### Runtime ###
data/
uploads/
*.log

### 개인키 / 인증서 ###
*.pem
*.key
*.p12
```

---

## 10. GitHub Actions

### `.github/workflows/ci-test.yml` (dev 대상)
```yaml
name: CI Test
on:
  pull_request:
    branches: [dev]
  push:
    branches: [dev]

jobs:
  code-test:
    name: 코드 테스트
    runs-on: ubuntu-latest
    services:
      redis:
        image: redis:7-alpine
        ports: ["6379:6379"]
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v6
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: '25'
      - run: chmod +x ./gradlew
      - name: 테스트 실행
        run: ./gradlew test --no-daemon
        env:
          SPRING_PROFILES_ACTIVE: test
          REDIS_HOST: localhost
          REDIS_PORT: 6379
```

### `.github/workflows/ci-cd.yml` (main 배포)
```yaml
name: CI/CD
on:
  push:
    branches: [main]

env:
  REGISTRY: ghcr.io

jobs:
  tagging:
    name: 태깅
    runs-on: ubuntu-latest
    permissions:
      contents: write
    outputs:
      tag_name: ${{ steps.tag_version.outputs.new_tag }}
    steps:
      - uses: actions/checkout@v6
      - id: tag_version
        uses: mathieudutour/github-tag-action@v6.2
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
      - uses: ncipollo/release-action@v1
        with:
          tag: ${{ steps.tag_version.outputs.new_tag }}
          name: ${{ steps.tag_version.outputs.new_tag }}
          body: ${{ steps.tag_version.outputs.changelog }}

  build-push-image:
    name: 이미지 빌드/푸시
    runs-on: ubuntu-latest
    needs: tagging
    permissions:
      contents: read
      packages: write
      attestations: write
      id-token: write
    steps:
      - uses: actions/checkout@v6
      - run: chmod +x ./gradlew
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: '25'
      - run: ./gradlew build -x test
      - uses: docker/login-action@v4
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GHCR_TOKEN }}
      - id: meta
        uses: docker/metadata-action@v6
        with:
          images: ${{ env.REGISTRY }}/${{ github.repository }}
          tags: |
            type=raw,value=${{ needs.tagging.outputs.tag_name }}
            type=raw,value=latest
      - uses: docker/build-push-action@v7
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}

  deploy:
    runs-on: self-hosted          # EC2에 등록한 runner
    needs: build-push-image
    steps:
      - uses: actions/checkout@v6
      - name: 레지스트리 로그인
        run: echo "$GHCR_TOKEN" | sudo docker login ghcr.io -u "${{ github.actor }}" --password-stdin
        env:
          GHCR_TOKEN: ${{ secrets.GHCR_TOKEN }}
      - name: 배포
        run: |
          sudo cp docker-compose.yml /home/ubuntu/docker-compose.yml
          cd /home/ubuntu
          sudo docker compose --env-file .env.production pull
          sudo docker compose --env-file .env.production up -d --no-build
```

**필요한 Secret**: `GHCR_TOKEN`(권한 `read:packages`, `write:packages`). `GITHUB_TOKEN`은 자동으로 제공된다.

---

## 11. `common/config` Java 설정 클래스

### SecurityConfig (Stateless JWT + OAuth2 + 메서드별 EndPoints 화이트리스트)
```java
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final OauthSuccessHandler oauthSuccessHandler;
    private final JwtFilter jwtFilter;

    @Value("${custom.baseUrl}")
    private String baseUrl;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))          // h2-console
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2Login(o -> o.successHandler(oauthSuccessHandler))
                // /api/** 는 302 리다이렉트 대신 상태코드만 (HTTPS Mixed Content 방지)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            if (req.getRequestURI().startsWith("/api/")) { res.sendError(401); return; }
                            res.sendRedirect(baseUrl + "/login");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            if (req.getRequestURI().startsWith("/api/")) { res.sendError(403); return; }
                            res.sendRedirect(baseUrl + "/login");
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/**").permitAll()                 // swagger (운영에선 제한 검토)
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        .requestMatchers(HttpMethod.POST, EndPoints.POST_ANONYMOUS).anonymous()
                        .requestMatchers(HttpMethod.GET,  EndPoints.GET_ANONYMOUS).anonymous()
                        .requestMatchers(HttpMethod.POST, EndPoints.POST_PERMIT_ALL).permitAll()
                        .requestMatchers("/api/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.GET,    EndPoints.AUTHENTICATED).authenticated()
                        .requestMatchers(HttpMethod.POST,   EndPoints.AUTHENTICATED).authenticated()
                        .requestMatchers(HttpMethod.PUT,    EndPoints.AUTHENTICATED).authenticated()
                        .requestMatchers(HttpMethod.PATCH,  EndPoints.AUTHENTICATED).authenticated()
                        .requestMatchers(HttpMethod.DELETE, EndPoints.AUTHENTICATED).authenticated()
                        .anyRequest().denyAll())                                  // 화이트리스트 방식
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override public void addCorsMappings(CorsRegistry r) {
                r.addMapping("/api/**").allowedOrigins(baseUrl).allowedMethods("*").allowedHeaders("*")
                 .allowCredentials(true).exposedHeaders("Authorization").maxAge(3600);
            }
        };
    }

    public static class EndPoints {
        public static final String[] GET_ANONYMOUS   = { "/oauth2/authorization/**" };
        public static final String[] POST_ANONYMOUS  = { "/api/users/login", "/api/users/signup" };
        public static final String[] POST_PERMIT_ALL = { "/api/users/refresh" };
        public static final String[] AUTHENTICATED   = { "/api/users/**", "/api/{resources}/**" /* 도메인 추가 */ };
    }

    // Role 계층: SUPER_ADMIN > ADMIN > USER
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("ROLE_SUPER_ADMIN > ROLE_ADMIN\nROLE_ADMIN > ROLE_USER");
    }
}
```

### JwtFilter (요약)
```java
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final TokenProvider tokenProvider;
    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String token = extractToken(req);                    // "Bearer " 접두사 제거
        if (token != null && tokenProvider.validate(token)) {
            TokenBody body = tokenProvider.parseJwt(token);
            CurrentUser user = userService.loadCurrentUserByEmail(body.email());
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        }
        chain.doFilter(req, res);
    }
}
```
- `TokenProvider`: JJWT로 Access/Refresh 토큰을 발급, 검증, 파싱한다. 키는 `JwtProperties.secrets.appkey`(32바이트 이상)를 쓴다.
- Refresh 토큰은 `RefreshToken`(Redis `@RedisHash`, TTL = refresh 유효기간)에 저장한다.
- `OauthService`(OAuth2UserService)와 `OauthSuccessHandler`: 이메일로 사용자를 찾거나 생성하고, JWT를 발급한 뒤 `{baseUrl}/oauth/callback?token=...`으로 리다이렉트한다.

### Properties
```java
@Getter @RequiredArgsConstructor
@ConfigurationProperties(prefix = "custom.jwt")
public class JwtProperties {
    private final Payload payload; private final Secrets secrets; private final Validations validations;
    @Getter @RequiredArgsConstructor public static class Payload {
        private final String issuer, subjectAccessToken, subjectRefreshToken, audiance; }
    @Getter @RequiredArgsConstructor public static class Secrets { private final String appkey, vanillakey; }
    @Getter @RequiredArgsConstructor public static class Validations { private final Long access, refresh; }
}
// RedisProperties: prefix = "custom.redis" (host, port)
// Application 클래스에 @ConfigurationPropertiesScan 추가
```

### JpaConfig / QueryDslConfiguration / BaseEntity
```java
@Configuration @EnableJpaAuditing
public class JpaConfig {}

@Configuration
public class QueryDslConfiguration {
    @PersistenceContext private EntityManager em;
    @Bean public JPAQueryFactory jpaQueryFactory() { return new JPAQueryFactory(em); }
}

@MappedSuperclass @Getter
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @LastModifiedDate @Column(nullable = false) private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;                          // soft delete
    public void softDelete() { this.deletedAt = LocalDateTime.now(); }
}
```

### RedisConfiguration
```java
@Configuration @RequiredArgsConstructor
public class RedisConfiguration {
    private final RedisProperties props;

    @Bean public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://%s:%s".formatted(props.getHost(), props.getPort()));
        return Redisson.create(config);
    }
    @Bean public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(props.getHost(), Integer.parseInt(props.getPort()));
    }
}
```

### S3Config
```java
@Configuration
public class S3Config {
    @Value("${custom.s3.access-key}") private String accessKey;
    @Value("${custom.s3.secret-key}") private String secretKey;
    @Value("${spring.cloud.aws.region.static}") private String region;

    private StaticCredentialsProvider creds() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }
    @Bean public S3Client s3Client() {
        return S3Client.builder().region(Region.of(region)).credentialsProvider(creds()).build();
    }
    @Bean public S3Presigner s3Presigner() {                 // 다운로드는 presigned URL로
        return S3Presigner.builder().region(Region.of(region)).credentialsProvider(creds()).build();
    }
}
```
파일 키 규칙 예: `{purpose}/{yyyy}/{MM}/{uuid}.{ext}` (`S3FileKeyGenerator`).

### SwaggerConfig
```java
@Configuration
@OpenAPIDefinition(info = @Info(title = "{{PROJECT_NAME}}", version = "0.0.1", description = "{{PROJECT_NAME}} API 명세"))
public class SwaggerConfig {
    @Bean public OpenAPI openAPI() {
        SecurityScheme bearer = new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT");
        return new OpenAPI().components(new Components().addSecuritySchemes("bearerAuth", bearer))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
    @Bean public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder().group("User API")
                .pathsToMatch("/api/users/**", "/api/admin/users/**").build();
    }
    // 도메인마다 GroupedOpenApi 하나씩. 여러 경로는 pathsToMatch(a, b) 한 번에 (연속 호출 시 덮어쓰기 주의)
}
```

### PasswordEncoderConfig / AdminBootstrapRunner
```java
@Configuration
public class PasswordEncoderConfig {
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
// AdminBootstrapRunner: ApplicationRunner. env(ADMIN_EMAIL/ADMIN_PASSWORD)로 최초 SUPER_ADMIN을 생성하고, 이미 있으면 skip
```

---

## 12. 서버 배포 절차 (`DEPLOY.md` 요약)

1. **서버 준비 (EC2 Ubuntu)**: Docker와 compose plugin을 설치하고, `usermod -aG docker ubuntu`를 실행하고, 작은 인스턴스라면 4G swap을 추가한다(`vm.swappiness=10`).
2. **env 파일 배치**: `.env.production.example`을 `/home/ubuntu/.env.production`으로 복사하고 `CHANGE_ME` 값을 모두 교체한다.
3. **self-hosted runner** 등록: 레포 Settings → Actions → Runners에서 등록한다.
4. **Secret** 등록: `GHCR_TOKEN`.
5. **보안 그룹**: 80/443은 전체 허용, 81(NPM UI)과 22(SSH)는 관리자 IP `/32`만 허용한다.
6. **NPM Proxy Host**: `{{DOMAIN_NAME}}`을 `http://frontend:80`으로 연결하고, SSL 탭에서 Let's Encrypt 인증서 발급, Force SSL, HTTP/2를 켠다. 그 전에 도메인 A 레코드가 Elastic IP를 가리켜야 한다.
7. **수동 확인**:
   ```bash
   cd /home/ubuntu && sudo docker compose --env-file .env.production pull \
     && sudo docker compose --env-file .env.production up -d --no-build && sudo docker compose ps
   ```

---

## 13. 원본에서 발견한 개선점 (새 프로젝트에선 처음부터 반영)

| 항목 | 원본 상태 | 권장 |
|---|---|---|
| `ddl-auto` | 기본 yml이 `create-drop`(운영에서도 재기동 시 데이터 삭제) | env `DDL_AUTO`로 분리하고, 운영은 `validate` + Flyway |
| `show-sql` | true 고정 | 운영 false |
| Security permitAll | `/h2-console/**`, `/api/files/**`, `/api/actuator/**`를 개발 편의로 개방 | 프로파일별로 분리하고, 운영에서는 제거 |
| Swagger 그룹 | `pathsToMatch()`를 연속 호출해서 앞의 경로가 덮어써짐 | `pathsToMatch(a, b)` 한 번에 호출 |
| 의존성 | `mysql-connector-j`가 중복으로 선언됨 | 하나만 남김 |
| env 키 | `REDIS_HOST`와 `redis.host`가 이중으로 존재하고, camelCase와 UPPER가 섞임 | UPPER_SNAKE로 통일 권장 |
| ApiResponse | `error`나 `data`가 null이어도 직렬화됨 | `@JsonInclude(NON_NULL)` |
| 예외 핸들러 | `Exception` 전체 핸들러와 `fields[]`가 없음 | 500 `INTERNAL_ERROR` 핸들러와 필드 에러 목록 추가 |
| PageResponse | 문서에만 있고 코드에는 없음 | `common/dto/PageResponse` 구현 |
| Actuator | Swagger 그룹만 있고 의존성이 없음 | 필요하면 actuator + prometheus 추가 |
| 배포 단계 테스트 | main 배포에서 `-x test` | dev CI에서 테스트를 보장하거나, main에서도 빠른 테스트를 돌림 |
