package com.gorapaduk.makingpet.reward.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.gorapaduk.makingpet.item.service.InventoryService;
import com.gorapaduk.makingpet.item.service.ItemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HomeRewardApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired ItemService itemService;
    @Autowired InventoryService inventoryService;

    record User(UUID id, String nickname, String token) {}

    private User signUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String email = "h" + suffix + "@example.com";
        String nickname = "홈" + suffix;
        var reg = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "password1234", "nickname": "%s" }""".formatted(email, nickname)))
                .andExpect(status().isCreated()).andReturn();
        UUID id = UUID.fromString(om.readTree(reg.getResponse().getContentAsString()).path("data").path("id").asText());
        var login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "password1234" }""".formatted(email)))
                .andExpect(status().isOk()).andReturn();
        return new User(id, nickname, om.readTree(login.getResponse().getContentAsString()).path("data").path("accessToken").asText());
    }

    // ----- Home -----

    @Test
    @DisplayName("H-1 가입하면 기본 테마가 적용된 마이 홈이 있고, 테마는 reserve 되어 거래 가능 수량이 0이다")
    void homeAfterSignup() throws Exception {
        User u = signUp();
        mvc.perform(get("/api/v1/home").header("Authorization", "Bearer " + u.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.theme.code").value("theme_basic"))
                .andExpect(jsonPath("$.data.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.data.furnitureLimit").value(20))
                .andExpect(jsonPath("$.data.representativePet").doesNotExist());
        mvc.perform(get("/api/v1/inventory").header("Authorization", "Bearer " + u.token()))
                .andExpect(jsonPath("$.data.items[0].reserved").value(1))
                .andExpect(jsonPath("$.data.items[0].available").value(0));
    }

    @Test
    @DisplayName("H-3 가구 배치는 보유 수량만큼만 되고, 전체 교체 시 reserve 가 정확히 맞는다")
    void furniture() throws Exception {
        User u = signUp();
        UUID rug = itemService.getByCode("fluffy_rug").getId();
        inventoryService.grant(u.id(), rug, 2);

        String two = """
                { "furniture": [ { "itemId": "%s", "x": 10, "y": 80, "layer": 0 }, { "itemId": "%s", "x": 60, "y": 80, "layer": 1 } ] }"""
                .formatted(rug, rug);
        mvc.perform(put("/api/v1/home/furniture").header("Authorization", "Bearer " + u.token())
                        .contentType(MediaType.APPLICATION_JSON).content(two))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.furniture", hasSize(2)));
        assertThat(inventoryService.available(u.id(), rug)).isZero();

        String three = """
                { "furniture": [ { "itemId": "%s", "x": 1, "y": 1, "layer": 0 }, { "itemId": "%s", "x": 2, "y": 2, "layer": 0 }, { "itemId": "%s", "x": 3, "y": 3, "layer": 0 } ] }"""
                .formatted(rug, rug, rug);
        mvc.perform(put("/api/v1/home/furniture").header("Authorization", "Bearer " + u.token())
                        .contentType(MediaType.APPLICATION_JSON).content(three))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVENTORY_INSUFFICIENT"));

        mvc.perform(put("/api/v1/home/furniture").header("Authorization", "Bearer " + u.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"furniture\": [] }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.furniture", hasSize(0)));
        assertThat(inventoryService.available(u.id(), rug)).isEqualTo(2);
    }

    @Test
    @DisplayName("H-4/H-5 공개 홈은 익명도 볼 수 있고, 비공개로 바꾸면 남은 403 HOME_PRIVATE, 본인은 200")
    void visibilityAndVisit() throws Exception {
        User owner = signUp();
        mvc.perform(get("/api/v1/homes/" + owner.nickname()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.owner.nickname").value(owner.nickname()))
                .andExpect(jsonPath("$.data.furnitureLimit").doesNotExist());

        mvc.perform(patch("/api/v1/home/visibility").header("Authorization", "Bearer " + owner.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"visibility\": \"PRIVATE\" }"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/homes/" + owner.nickname()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("HOME_PRIVATE"));
        mvc.perform(get("/api/v1/homes/" + owner.nickname()).header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/homes/없는닉네임"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HOME_NOT_FOUND"));
    }

    @Test
    @DisplayName("H-6 좋아요는 남의 공개 홈에 하루 1번")
    void like() throws Exception {
        User owner = signUp();
        User fan = signUp();
        mvc.perform(post("/api/v1/homes/" + owner.nickname() + "/likes").header("Authorization", "Bearer " + fan.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(1));
        mvc.perform(post("/api/v1/homes/" + owner.nickname() + "/likes").header("Authorization", "Bearer " + fan.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("HOME_ALREADY_LIKED_TODAY"));
        mvc.perform(post("/api/v1/homes/" + owner.nickname() + "/likes").header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("HOME_SELF_LIKE"));
    }

    // ----- Reward -----

    @Test
    @DisplayName("R-2 출석은 하루 1회 30코인, 두 번째는 409, today 와 members/me 에 반영")
    void attendance() throws Exception {
        User u = signUp();
        mvc.perform(post("/api/v1/rewards/attendance").header("Authorization", "Bearer " + u.token()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.streak").value(1))
                .andExpect(jsonPath("$.data.coins").value(30))
                .andExpect(jsonPath("$.data.bonusCoins").value(0))
                .andExpect(jsonPath("$.data.wallet.balance").value(330));
        mvc.perform(post("/api/v1/rewards/attendance").header("Authorization", "Bearer " + u.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("REWARD_ALREADY_CLAIMED"));
        mvc.perform(get("/api/v1/rewards/today").header("Authorization", "Bearer " + u.token()))
                .andExpect(jsonPath("$.data.attendance.claimed").value(true))
                .andExpect(jsonPath("$.data.attendance.nextBonusIn").value(6))
                .andExpect(jsonPath("$.data.slot.remaining").value(5));
        mvc.perform(get("/api/v1/members/me").header("Authorization", "Bearer " + u.token()))
                .andExpect(jsonPath("$.data.today.attendance.claimed").value(true))
                .andExpect(jsonPath("$.data.today.rps.claimed").value(false));
        mvc.perform(get("/api/v1/rewards/attendance").header("Authorization", "Bearer " + u.token()))
                .andExpect(jsonPath("$.data.days", hasSize(1)))
                .andExpect(jsonPath("$.data.totalThisMonth").value(1));
    }

    @Test
    @DisplayName("R-4 가위바위보는 결과에 맞는 코인을 주고 하루 1회")
    void rps() throws Exception {
        User u = signUp();
        var res = mvc.perform(post("/api/v1/rewards/rps").header("Authorization", "Bearer " + u.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"hand\": \"ROCK\" }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.myHand").value("ROCK"))
                .andExpect(jsonPath("$.data.result", in(List.of("WIN", "DRAW", "LOSE"))))
                .andReturn();
        JsonNode d = om.readTree(res.getResponse().getContentAsString()).path("data");
        int expected = switch (d.path("result").asText()) { case "WIN" -> 50; case "DRAW" -> 20; default -> 10; };
        assertThat(d.path("coins").asInt()).isEqualTo(expected);
        assertThat(d.path("wallet").path("balance").asLong()).isEqualTo(300 + expected);
        mvc.perform(post("/api/v1/rewards/rps").header("Authorization", "Bearer " + u.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"hand\": \"PAPER\" }"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("R-5 슬롯은 5회까지, 6번째는 429 + Retry-After, 잔액은 참가비·배당과 정확히 맞는다")
    void slot() throws Exception {
        User u = signUp();
        long balance = 300;
        for (int i = 1; i <= 5; i++) {
            var res = mvc.perform(post("/api/v1/rewards/slot").header("Authorization", "Bearer " + u.token())
                            .header("Idempotency-Key", UUID.randomUUID().toString()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.reels", hasSize(3)))
                    .andExpect(jsonPath("$.data.entryFee").value(20))
                    .andExpect(jsonPath("$.data.remaining").value(5 - i))
                    .andReturn();
            JsonNode d = om.readTree(res.getResponse().getContentAsString()).path("data");
            balance += d.path("net").asLong();
            assertThat(d.path("wallet").path("balance").asLong()).isEqualTo(balance);
        }
        mvc.perform(post("/api/v1/rewards/slot").header("Authorization", "Bearer " + u.token())
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error.code").value("REWARD_DAILY_LIMIT_EXCEEDED"));
        mvc.perform(get("/api/v1/wallet").header("Authorization", "Bearer " + u.token()))
                .andExpect(jsonPath("$.data.balance").value(balance));
    }
}
