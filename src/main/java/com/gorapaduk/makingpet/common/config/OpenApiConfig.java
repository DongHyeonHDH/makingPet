package com.gorapaduk.makingpet.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc-openapi. 문서: /v3/api-docs, UI: /v3/swagger-ui (application.yaml 의 springdoc.*).
 * UI 의 Authorize 버튼에 로그인으로 받은 accessToken 을 넣으면 🔑 API 를 호출할 수 있다.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    OpenAPI makingPetOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("makingPet API")
                        .version("v1")
                        .description("펫 키우기 · 뽑기 · 마이 홈 · 보상 API. 명세는 docs/기획/04_api 를 기준으로 한다."))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
