package com.gorapaduk.makingpet.member.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/** 회원 API 가 01_API_표준의 ApiResponse 형식과 12_에러_코드_사전의 코드를 지키는지 확인한다. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private static final String REGISTER = """
            { "email": "%s", "password": "password1234", "nickname": "%s" }""";

    private MvcResult register(String email, String nickname) throws Exception {
        return mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER.formatted(email, nickname)))
                .andReturn();
    }

    private String login(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "password1234" }""".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andReturn();
        JsonNode body = om.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("accessToken").asText();
    }

    @Nested
    @DisplayName("Describe_회원가입은")
    class Register {

        @Test
        @DisplayName("It_성공하면_201과_ApiResponse_래퍼로_회원정보를_돌려준다")
        void success() throws Exception {
            mvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REGISTER.formatted("Duck@Example.com", "오리사랑")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.email").value("duck@example.com"))
                    .andExpect(jsonPath("$.data.nickname").value("오리사랑"))
                    .andExpect(jsonPath("$.data.role").value("USER"))
                    .andExpect(jsonPath("$.error").doesNotExist());
        }

        @Test
        @DisplayName("Context_이메일이_중복인_경우_It_409_MEMBER_EMAIL_DUPLICATED")
        void duplicatedEmail() throws Exception {
            register("dup@example.com", "첫번째");
            mvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REGISTER.formatted("dup@example.com", "두번째")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("MEMBER_EMAIL_DUPLICATED"));
        }

        @Test
        @DisplayName("Context_검증_실패인_경우_It_400_VALIDATION_FAILED와_fields")
        void validation() throws Exception {
            mvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "email": "not-an-email", "password": "short", "nickname": "" }"""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.error.fields", hasSize(3)));
        }
    }

    @Nested
    @DisplayName("Describe_로그인은")
    class Login {

        @Test
        @DisplayName("Context_비밀번호가_틀린_경우_It_401_INVALID_CREDENTIALS")
        void wrongPassword() throws Exception {
            register("login@example.com", "로그인");
            mvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "email": "login@example.com", "password": "wrong-password" }"""))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
        }
    }

    @Nested
    @DisplayName("Describe_내_정보는")
    class Me {

        @Test
        @DisplayName("Context_토큰이_없는_경우_It_401_TOKEN_REQUIRED를_ApiResponse_형식으로")
        void noToken() throws Exception {
            mvc.perform(get("/api/v1/members/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("WWW-Authenticate", "Bearer"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("TOKEN_REQUIRED"));
        }

        @Test
        @DisplayName("Context_로그인한_경우_It_조회와_닉네임_변경이_된다")
        void readAndUpdate() throws Exception {
            register("me@example.com", "나야");
            String token = login("me@example.com");

            mvc.perform(get("/api/v1/members/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nickname").value("나야"));

            mvc.perform(patch("/api/v1/members/me")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "nickname": "바꿨어" }"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nickname").value("바꿨어"));
        }

        @Test
        @DisplayName("Context_관리자_경로를_USER가_부르는_경우_It_403_FORBIDDEN")
        void adminForbidden() throws Exception {
            register("user@example.com", "일반유저");
            String token = login("user@example.com");
            mvc.perform(get("/api/v1/admin/items").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }
    }
}
