package com.gorapaduk.makingpet.gacha.integration;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.gorapaduk.makingpet.gacha.service.GachaPoolSeeder;
import com.gorapaduk.makingpet.item.service.InventoryService;
import com.gorapaduk.makingpet.item.service.ItemService;
import com.gorapaduk.makingpet.member.service.MemberService;
import com.gorapaduk.makingpet.pet.service.PetService;
import com.gorapaduk.makingpet.pet.service.PetSpeciesService;
import org.junit.jupiter.api.BeforeEach;
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
class PetGachaApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired PetService petService;
    @Autowired PetSpeciesService speciesService;
    @Autowired ItemService itemService;
    @Autowired InventoryService inventoryService;
    @Autowired MemberService memberService;

    private String token;
    private UUID memberId;
    private String poolId;

    @BeforeEach
    void setUp() throws Exception {
        String email = "g" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        var reg = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "password1234", "nickname": "%s" }"""
                                .formatted(email, "뽑" + email.substring(1, 7))))
                .andExpect(status().isCreated()).andReturn();
        memberId = UUID.fromString(om.readTree(reg.getResponse().getContentAsString()).path("data").path("id").asText());
        var login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "password1234" }""".formatted(email)))
                .andExpect(status().isOk()).andReturn();
        token = om.readTree(login.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        var pools = mvc.perform(get("/api/v1/gacha/pools")).andExpect(status().isOk()).andReturn();
        JsonNode pool = om.readTree(pools.getResponse().getContentAsString()).path("data").path("items").get(0);
        poolId = pool.path("id").asText();
    }

    private JsonNode draw(int count, String key) throws Exception {
        var res = mvc.perform(post("/api/v1/gacha/pools/" + poolId + "/draws")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"count\": " + count + " }"))
                .andReturn();
        return om.readTree(res.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("G-1 풀은 익명으로 조회되고 희귀도별 확률 합이 1 이다")
    void poolRates() throws Exception {
        mvc.perform(get("/api/v1/gacha/pools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].name").value(GachaPoolSeeder.STANDARD_POOL_NAME))
                .andExpect(jsonPath("$.data.items[0].rates[0].rarity").value("COMMON"))
                .andExpect(jsonPath("$.data.items[0].rates[0].rate", closeTo(0.60, 0.0001)))
                .andExpect(jsonPath("$.data.items[0].rates[2].rate", closeTo(0.12, 0.0001)));
    }

    @Test
    @DisplayName("G-2 첫 뽑기: 100코인 차감, 새 펫, 자동 대표 펫, 같은 키 재요청은 같은 응답")
    void firstDrawAndIdempotency() throws Exception {
        String key = UUID.randomUUID().toString();
        JsonNode first = draw(1, key);
        org.assertj.core.api.Assertions.assertThat(first.path("success").asBoolean()).isTrue();
        org.assertj.core.api.Assertions.assertThat(first.path("data").path("cost").asInt()).isEqualTo(100);
        org.assertj.core.api.Assertions.assertThat(first.path("data").path("results").get(0).path("resultType").asText()).isEqualTo("NEW_PET");
        org.assertj.core.api.Assertions.assertThat(first.path("data").path("results").get(0).path("pet").path("isRepresentative").asBoolean()).isTrue();
        org.assertj.core.api.Assertions.assertThat(first.path("data").path("wallet").path("balance").asLong()).isEqualTo(200);

        JsonNode replay = draw(1, key);                                  // 같은 키 → 처리하지 않고 최초 응답
        org.assertj.core.api.Assertions.assertThat(replay.path("data").path("batchId").asText())
                .isEqualTo(first.path("data").path("batchId").asText());
        mvc.perform(get("/api/v1/wallet").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.balance").value(200));

        mvc.perform(get("/api/v1/members/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.petCount").value(1))
                .andExpect(jsonPath("$.data.representativePet.id").value(first.path("data").path("results").get(0).path("pet").path("id").asText()));
    }

    @Test
    @DisplayName("G-2 Idempotency-Key 가 없으면 400, 잔액이 모자라면 400 WALLET_INSUFFICIENT_BALANCE")
    void drawGuards() throws Exception {
        mvc.perform(post("/api/v1/gacha/pools/" + poolId + "/draws")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"count\": 1 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_KEY_REQUIRED"));

        JsonNode ten = draw(10, UUID.randomUUID().toString());             // 900 > 300
        org.assertj.core.api.Assertions.assertThat(ten.path("error").path("code").asText()).isEqualTo("WALLET_INSUFFICIENT_BALANCE");
        mvc.perform(get("/api/v1/wallet").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.balance").value(300));
    }

    @Test
    @DisplayName("G-5 모든 종을 이미 가진 상태에서 뽑으면 REFUND 와 희귀도별 환급 코인")
    void duplicateRefund() throws Exception {
        for (var s : speciesService.listActive()) petService.create(memberId, s, "TEST");
        JsonNode res = draw(1, UUID.randomUUID().toString());
        JsonNode r = res.path("data").path("results").get(0);
        org.assertj.core.api.Assertions.assertThat(r.path("resultType").asText()).isEqualTo("REFUND");
        int expected = switch (r.path("species").path("rarity").asText()) {
            case "COMMON" -> 20; case "RARE" -> 50; case "EPIC" -> 150; default -> 500; };
        org.assertj.core.api.Assertions.assertThat(r.path("refundCoins").asInt()).isEqualTo(expected);
        org.assertj.core.api.Assertions.assertThat(res.path("data").path("wallet").path("balance").asLong()).isEqualTo(300 - 100 + expected);
    }

    @Test
    @DisplayName("P-5 상호작용은 기분·친밀도를 올리고, 쿨다운 중 재요청은 429 + Retry-After")
    void interactionCooldown() throws Exception {
        String petId = draw(1, UUID.randomUUID().toString()).path("data").path("results").get(0).path("pet").path("id").asText();
        mvc.perform(post("/api/v1/pets/" + petId + "/interactions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"type\": \"PLAY\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.moodDelta").value(15))
                .andExpect(jsonPath("$.data.affectionDelta").value(3))
                .andExpect(jsonPath("$.data.motionKey").value("motion.play"))
                .andExpect(jsonPath("$.data.pet.mood").value(85));
        mvc.perform(post("/api/v1/pets/" + petId + "/interactions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"type\": \"PLAY\" }"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error.code").value("PET_INTERACTION_COOLDOWN"));
        mvc.perform(get("/api/v1/pets/" + petId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.affection").value(3))
                .andExpect(jsonPath("$.data.nextLevelAffection").value(30))
                .andExpect(jsonPath("$.data.cooldowns.PLAY").exists())
                .andExpect(jsonPath("$.data.cooldowns.PET").doesNotExist());
    }

    @Test
    @DisplayName("P-6 장착: 카테고리가 맞으면 reserved 가 늘고, 슬롯 불일치는 400, 수량 부족은 400")
    void equipment() throws Exception {
        String petId = draw(1, UUID.randomUUID().toString()).path("data").path("results").get(0).path("pet").path("id").asText();
        UUID ribbon = itemService.getByCode("ribbon").getId();
        inventoryService.grant(memberId, ribbon, 1);

        mvc.perform(put("/api/v1/pets/" + petId + "/equipment").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"ACCESSORY_1\": \"" + ribbon + "\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.equipment.ACCESSORY_1.code").value("ribbon"))
                .andExpect(jsonPath("$.data.equipment.MOTION.code").value("motion_idle"));
        org.assertj.core.api.Assertions.assertThat(inventoryService.available(memberId, ribbon)).isZero();

        mvc.perform(put("/api/v1/pets/" + petId + "/equipment").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"TOP\": \"" + ribbon + "\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ITEM_SLOT_MISMATCH"));

        mvc.perform(put("/api/v1/pets/" + petId + "/equipment").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"ACCESSORY_1\": \"" + ribbon + "\", \"ACCESSORY_2\": \"" + ribbon + "\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVENTORY_INSUFFICIENT"));

        mvc.perform(put("/api/v1/pets/" + petId + "/equipment").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{ }"))
                .andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(inventoryService.available(memberId, ribbon)).isEqualTo(1);
    }
}
