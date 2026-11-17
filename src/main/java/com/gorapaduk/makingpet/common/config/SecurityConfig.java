package com.gorapaduk.makingpet.common.config;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import tools.jackson.databind.ObjectMapper;
import com.gorapaduk.makingpet.common.constants.ErrorCode;
import com.gorapaduk.makingpet.common.dto.ApiResponse;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless JWT(HS256) 리소스 서버.
 * - 익명 허용 경로는 기획/04_api/02_엔드포인트_목록.md 의 🔓 와 일치시킨다.
 * - 401/403 도 ApiResponse 실패 형식으로 쓴다.
 * - JWT 의 role 클레임 → ROLE_{role} 권한. 계층: SUPER_ADMIN > ADMIN > USER.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    public static final String ISSUER = "makingpet";
    public static final String ROLE_CLAIM = "role";

    private final ObjectMapper objectMapper;

    /** 익명 허용 엔드포인트 화이트리스트. 새 🔓 엔드포인트는 문서와 함께 여기에 추가한다. */
    public static final class EndPoints {
        public static final String[] POST_ANONYMOUS = {
                "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh" };
        public static final String[] GET_PUBLIC = {
                "/api/v1/gacha/pools",
                "/api/v1/items",
                "/api/v1/homes/{nickname}",
                "/api/v1/shop/products", "/api/v1/shop/products/{productId}",
                "/api/v1/auction/listings", "/api/v1/auction/listings/{listingId}", "/api/v1/auction/prices",
                "/api/v1/notices", "/api/v1/notices/{noticeId}",
                "/assets/**" };
        public static final String[] DEV_ONLY = { "/h2-console/**", "/v3/api-docs/**", "/v3/swagger-ui/**" };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))     // h2-console
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, EndPoints.POST_ANONYMOUS).permitAll()
                        .requestMatchers(HttpMethod.GET, EndPoints.GET_PUBLIC).permitAll()
                        .requestMatchers(EndPoints.DEV_ONLY).permitAll()                 // TODO 운영 프로파일에서 제거
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(this::unauthorized)
                        .accessDeniedHandler((req, res, e) -> writeError(res, ErrorCode.FORBIDDEN)))
                .oauth2ResourceServer(rs -> rs
                        .authenticationEntryPoint(this::unauthorized)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("ROLE_SUPER_ADMIN > ROLE_ADMIN\nROLE_ADMIN > ROLE_USER");
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString(ROLE_CLAIM);
            return role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));   // TODO 운영 도메인은 env
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Retry-After"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    // ----- JWT -----

    @Bean
    SecretKey signingKey(@Value("${makingpet.jwt.secret:}") String configuredSecret) {
        byte[] bytes;
        if (configuredSecret.isBlank()) {
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);            // 로컬 개발: 재시작마다 토큰 무효화
        } else {
            bytes = Base64.getDecoder().decode(configuredSecret);
        }
        if (bytes.length < 32) {
            throw new IllegalArgumentException("makingpet.jwt.secret 은 32바이트 이상을 Base64 로 인코딩한 값이어야 합니다.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key) {
        var decoder = NimbusJwtDecoder.withSecretKey(key).build();
        decoder.setJwtValidator(jwt -> {
            try {
                UUID.fromString(jwt.getSubject());
            } catch (IllegalArgumentException | NullPointerException e) {
                return invalid("invalid_token");
            }
            Instant now = Instant.now();
            if (!ISSUER.equals(jwt.getClaimAsString("iss"))
                    || jwt.getExpiresAt() == null
                    || (jwt.getNotBefore() != null && jwt.getNotBefore().isAfter(now))) {
                return invalid("invalid_token");
            }
            return jwt.getExpiresAt().isAfter(now) ? OAuth2TokenValidatorResult.success() : invalid("token_expired");
        });
        return decoder;
    }

    private static OAuth2TokenValidatorResult invalid(String code) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(code));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    // ----- 에러 응답 -----

    private void unauthorized(HttpServletRequest request, HttpServletResponse response, Exception failure)
            throws IOException {
        boolean expired = Stream.iterate((Throwable) failure, c -> c != null, Throwable::getCause)
                .anyMatch(c -> c instanceof JwtValidationException v
                        && v.getErrors().stream().anyMatch(err -> "token_expired".equals(err.getErrorCode())));
        boolean missing = request.getHeader("Authorization") == null;
        response.setHeader("WWW-Authenticate", missing ? "Bearer" : "Bearer error=\"invalid_token\"");
        writeError(response, expired ? ErrorCode.TOKEN_EXPIRED : missing ? ErrorCode.TOKEN_REQUIRED : ErrorCode.INVALID_TOKEN);
    }

    private void writeError(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(code));
    }
}
