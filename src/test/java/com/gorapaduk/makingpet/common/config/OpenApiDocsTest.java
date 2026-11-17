package com.gorapaduk.makingpet.common.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** springdoc-openapi: 문서(/v3/api-docs)와 UI(/v3/swagger-ui)가 토큰 없이 열린다. */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest {

    @Autowired MockMvc mvc;

    @Test
    @DisplayName("/v3/api-docs 는 익명으로 200, API 정보·JWT 스킴·/api/v1 경로가 들어 있다")
    void apiDocs() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("makingPet API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.paths['/api/v1/items'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post").exists());
    }

    @Test
    @DisplayName("/v3/swagger-ui 는 익명으로 index.html 로 이동하고, 페이지가 열린다")
    void swaggerUi() throws Exception {
        mvc.perform(get("/v3/swagger-ui"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/v3/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("swagger-ui")));
    }
}
