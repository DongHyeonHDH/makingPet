package com.gorapaduk.makingpet.shop.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.gorapaduk.makingpet.item.service.InventoryService;
import com.gorapaduk.makingpet.item.service.ItemService;
import com.gorapaduk.makingpet.member.domain.Role;
import com.gorapaduk.makingpet.member.repository.MemberRepository;
import com.gorapaduk.makingpet.wallet.service.WalletService;
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

/** W7: 상점(S-1~S-3) · 공지(C-1, C-2) · 관리자(AD-1~AD-5, AD-8~AD-12) · 시드 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ShopNoticeAdminApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired MemberRepository memberRepository;
    @Autowired ItemService itemService;
    @Autowired InventoryService inventoryService;
    @Autowired WalletService walletService;

    record User(UUID id, String nickname, String token) {}

    private User signUp(Role role) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String email = "s" + suffix + "@example.com";
        String nickname = "상점" + suffix;
        var reg = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "password1234", "nickname": "%s" }""".formatted(email, nickname)))
                .andExpect(status().isCreated()).andReturn();
        UUID id = UUID.fromString(om.readTree(reg.getResponse().getContentAsString()).path("data").path("id").asText());
        if (role != Role.USER) memberRepository.findById(id).orElseThrow().changeRole(role);   // 토큰의 role 클레임에 반영되도록 로그인 전에
        var login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "password1234" }""".formatted(email)))
                .andExpect(status().isOk()).andReturn();
        return new User(id, nickname, om.readTree(login.getResponse().getContentAsString()).path("data").path("accessToken").asText());
    }

    private static String bearer(User u) {
        return "Bearer " + u.token();
    }

    /** 상점 목록에서 아이템 code 로 상품 ID 를 찾는다 (시드 상품) */
    private String productIdOf(String itemCode) throws Exception {
        var res = mvc.perform(get("/api/v1/shop/products")).andExpect(status().isOk()).andReturn();
        for (JsonNode p : om.readTree(res.getResponse().getContentAsString()).path("data").path("items")) {
            if (itemCode.equals(p.path("item").path("code").asText())) return p.path("id").asText();
        }
        throw new AssertionError("상품 없음: " + itemCode);
    }

    // ----- Shop -----

    @Test
    @DisplayName("S-1 시드 상품 5종이 sort_order 순으로 익명에게도 보이고, 익명은 owned=null · 로그인은 보유 수량")
    void listProducts() throws Exception {
        mvc.perform(get("/api/v1/shop/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].item.code").value("ribbon"))
                .andExpect(jsonPath("$.data.items[0].price").value(70))
                .andExpect(jsonPath("$.data.items[*].item.code", hasItem("fluffy_rug")))
                .andExpect(jsonPath("$.data.items[*].owned", everyItem(nullValue())));

        User u = signUp(Role.USER);
        mvc.perform(get("/api/v1/shop/products").param("category", "FURNITURE").header("Authorization", bearer(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].item.category", everyItem(org.hamcrest.Matchers.is("FURNITURE"))))
                .andExpect(jsonPath("$.data.items[0].owned").value(0));
    }

    @Test
    @DisplayName("S-2 상세는 item.description 을 포함하고, 없는 상품은 404 SHOP_PRODUCT_NOT_FOUND")
    void productDetail() throws Exception {
        mvc.perform(get("/api/v1/shop/products/" + productIdOf("ribbon")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.item.name").value("딸기 리본"))
                .andExpect(jsonPath("$.data.item.description").isNotEmpty());
        mvc.perform(get("/api/v1/shop/products/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SHOP_PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("S-3 구매: 코인 차감 · 아이템 지급 · 원장 기록, 같은 Idempotency-Key 재시도는 한 번만 처리")
    void purchase() throws Exception {
        User u = signUp(Role.USER);
        String productId = productIdOf("ribbon");

        var first = mvc.perform(post("/api/v1/shop/products/" + productId + "/purchases").header("Authorization", bearer(u))
                        .header("Idempotency-Key", "buy-1").contentType(MediaType.APPLICATION_JSON).content("{ \"quantity\": 2 }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("딸기 리본 2개를 구매했습니다."))
                .andExpect(jsonPath("$.data.unitPrice").value(70))
                .andExpect(jsonPath("$.data.totalPrice").value(140))
                .andExpect(jsonPath("$.data.inventory.quantity").value(2))
                .andExpect(jsonPath("$.data.inventory.available").value(2))
                .andExpect(jsonPath("$.data.wallet.balance").value(160))
                .andReturn();
        String purchaseId = om.readTree(first.getResponse().getContentAsString()).path("data").path("purchaseId").asText();

        mvc.perform(post("/api/v1/shop/products/" + productId + "/purchases").header("Authorization", bearer(u))
                        .header("Idempotency-Key", "buy-1").contentType(MediaType.APPLICATION_JSON).content("{ \"quantity\": 2 }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.purchaseId").value(purchaseId));
        assertThat(walletService.getBalance(u.id()).balance()).isEqualTo(160);
        assertThat(inventoryService.available(u.id(), itemService.getByCode("ribbon").getId())).isEqualTo(2);

        mvc.perform(get("/api/v1/wallet/transactions").param("reason", "SHOP_PURCHASE").header("Authorization", bearer(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].amount").value(-140));

        mvc.perform(get("/api/v1/shop/products/" + productId).header("Authorization", bearer(u)))
                .andExpect(jsonPath("$.data.owned").value(2));
    }

    @Test
    @DisplayName("S-3 실패: 수량 범위 밖 400, 코인 부족 400(아무것도 안 바뀜), 키 없음 400")
    void purchaseFailures() throws Exception {
        User u = signUp(Role.USER);
        String tee = productIdOf("cloud_tee");      // 120 × 10 = 1200 > 300
        mvc.perform(post("/api/v1/shop/products/" + tee + "/purchases").header("Authorization", bearer(u))
                        .header("Idempotency-Key", "q-11").contentType(MediaType.APPLICATION_JSON).content("{ \"quantity\": 11 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        mvc.perform(post("/api/v1/shop/products/" + tee + "/purchases").header("Authorization", bearer(u))
                        .header("Idempotency-Key", "q-10").contentType(MediaType.APPLICATION_JSON).content("{ \"quantity\": 10 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("WALLET_INSUFFICIENT_BALANCE"));
        assertThat(walletService.getBalance(u.id()).balance()).isEqualTo(300);
        assertThat(inventoryService.available(u.id(), itemService.getByCode("cloud_tee").getId())).isZero();

        mvc.perform(post("/api/v1/shop/products/" + tee + "/purchases").header("Authorization", bearer(u)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_KEY_REQUIRED"));
    }

    // ----- Admin: 권한 · 상점 -----

    @Test
    @DisplayName("관리자 API: 익명 401, USER 403 FORBIDDEN, SUPER_ADMIN 은 ADMIN 권한 포함")
    void adminAuthorization() throws Exception {
        String body = "{ \"title\": \"t\", \"content\": \"c\" }";
        mvc.perform(post("/api/v1/admin/notices").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        User user = signUp(Role.USER);
        mvc.perform(post("/api/v1/admin/notices").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        User superAdmin = signUp(Role.SUPER_ADMIN);
        mvc.perform(post("/api/v1/admin/notices").header("Authorization", bearer(superAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("AD-8/AD-9 상품 등록·숨김: 숨기면 목록·상세에서 빠지고 구매는 409 SHOP_PRODUCT_NOT_ON_SALE")
    void adminProducts() throws Exception {
        User admin = signUp(Role.ADMIN);
        UUID boots = itemService.getByCode("yellow_boots").getId();
        var created = mvc.perform(post("/api/v1/admin/shop/products").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "itemId": "%s", "price": 55, "saleEndsAt": "2099-12-31T23:59:59+09:00", "visible": true, "sortOrder": 1 }"""
                                .formatted(boots)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.price").value(55))
                .andExpect(jsonPath("$.data.onSale").value(true))
                .andReturn();
        String productId = om.readTree(created.getResponse().getContentAsString()).path("data").path("id").asText();
        mvc.perform(get("/api/v1/shop/products"))
                .andExpect(jsonPath("$.data.items[0].id").value(productId))
                .andExpect(jsonPath("$.data.items[0].saleEndsAt").value("2099-12-31T23:59:59"));

        mvc.perform(patch("/api/v1/admin/shop/products/" + productId).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"visible\": false }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onSale").value(false));
        mvc.perform(get("/api/v1/shop/products"))
                .andExpect(jsonPath("$.data.items[*].id", not(hasItem(productId))));
        mvc.perform(get("/api/v1/shop/products/" + productId))
                .andExpect(status().isNotFound());
        User buyer = signUp(Role.USER);
        mvc.perform(post("/api/v1/shop/products/" + productId + "/purchases").header("Authorization", bearer(buyer))
                        .header("Idempotency-Key", "hidden").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SHOP_PRODUCT_NOT_ON_SALE"));
    }

    @Test
    @DisplayName("AD-8 검증: MOTION 아이템 400 ITEM_NOT_TRADABLE, 가격 0 400, 시작 ≥ 끝 400")
    void adminProductValidation() throws Exception {
        User admin = signUp(Role.ADMIN);
        UUID motion = itemService.getByCode("motion_idle").getId();
        UUID rug = itemService.getByCode("fluffy_rug").getId();
        mvc.perform(post("/api/v1/admin/shop/products").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"itemId\": \"%s\", \"price\": 10 }".formatted(motion)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ITEM_NOT_TRADABLE"));
        mvc.perform(post("/api/v1/admin/shop/products").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"itemId\": \"%s\", \"price\": 0 }".formatted(rug)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        mvc.perform(post("/api/v1/admin/shop/products").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                { "itemId": "%s", "price": 10, "saleStartsAt": "2027-01-02T00:00:00+09:00", "saleEndsAt": "2027-01-01T00:00:00+09:00" }"""
                                .formatted(rug)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    // ----- Notice -----

    @Test
    @DisplayName("C-1 시드 환영 공지(고정)가 익명 목록에 보인다")
    void seededNotice() throws Exception {
        mvc.perform(get("/api/v1/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].title", hasItem("makingPet 에 오신 것을 환영합니다")));
    }

    @Test
    @DisplayName("AD-10~12 공지: 고정 공지가 맨 위, 예약 공지는 숨김, 수정 반영, 삭제 후 404")
    void noticeLifecycle() throws Exception {
        User admin = signUp(Role.ADMIN);
        var created = mvc.perform(post("/api/v1/admin/notices").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"  새 친구 '공룡' 출시  \", \"content\": \"## 공룡\\n쿵쿵\", \"pinned\": true }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("새 친구 '공룡' 출시"))
                .andExpect(jsonPath("$.data.author.nickname").value(admin.nickname()))
                .andReturn();
        String id = om.readTree(created.getResponse().getContentAsString()).path("data").path("id").asText();

        var scheduled = mvc.perform(post("/api/v1/admin/notices").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"예약\", \"content\": \"미래\", \"publishedAt\": \"2099-01-01T00:00:00+09:00\" }"))
                .andExpect(status().isCreated()).andReturn();
        String scheduledId = om.readTree(scheduled.getResponse().getContentAsString()).path("data").path("id").asText();

        mvc.perform(get("/api/v1/notices"))
                .andExpect(jsonPath("$.data.items[0].id").value(id))
                .andExpect(jsonPath("$.data.items[0].pinned").value(true))
                .andExpect(jsonPath("$.data.items[*].id", not(hasItem(scheduledId))));
        mvc.perform(get("/api/v1/notices/" + scheduledId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOTICE_NOT_FOUND"));

        mvc.perform(get("/api/v1/notices/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("## 공룡\n쿵쿵"))
                .andExpect(jsonPath("$.data.author.nickname").value(admin.nickname()));

        mvc.perform(patch("/api/v1/admin/notices/" + id).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"pinned\": false, \"content\": \"수정됨\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pinned").value(false))
                .andExpect(jsonPath("$.data.content").value("수정됨"))
                .andExpect(jsonPath("$.data.title").value("새 친구 '공룡' 출시"));

        mvc.perform(delete("/api/v1/admin/notices/" + id).header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
        mvc.perform(get("/api/v1/notices/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/admin/notices/" + id).header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("AD-10 검증: 제목 101자 400")
    void noticeValidation() throws Exception {
        User admin = signUp(Role.ADMIN);
        mvc.perform(post("/api/v1/admin/notices").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"%s\", \"content\": \"c\" }".formatted("가".repeat(101))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    // ----- Admin: 카탈로그 -----

    @Test
    @DisplayName("AD-4/AD-5 아이템: 등록 201, 코드 중복 409, MOTION 은 tradable 강제 false, category 변경 400, 이름 변경 반영")
    void adminItems() throws Exception {
        User admin = signUp(Role.ADMIN);
        String code = "star_pin_" + UUID.randomUUID().toString().substring(0, 4);
        String body = """
                { "code": "%s", "name": "별 핀", "category": "ACCESSORY", "rarity": "RARE", "assetKey": "items/star_pin.png", "tradable": true }"""
                .formatted(code);
        var created = mvc.perform(post("/api/v1/admin/items").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value(code))
                .andExpect(jsonPath("$.data.tradable").value(true))
                .andReturn();
        String itemId = om.readTree(created.getResponse().getContentAsString()).path("data").path("id").asText();
        mvc.perform(post("/api/v1/admin/items").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ITEM_CODE_DUPLICATED"));

        mvc.perform(post("/api/v1/admin/items").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                { "code": "motion_spin_%s", "name": "빙글", "category": "MOTION", "rarity": "COMMON", "assetKey": "motion.spin", "tradable": true }"""
                                .formatted(code.substring(code.length() - 4))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tradable").value(false));

        mvc.perform(patch("/api/v1/admin/items/" + itemId).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"category\": \"TOP\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        mvc.perform(patch("/api/v1/admin/items/" + itemId).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"name\": \"반짝 별 핀\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("반짝 별 핀"));
        mvc.perform(post("/api/v1/admin/items").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                { "code": "Bad-Code", "name": "x", "category": "TOP", "rarity": "COMMON", "assetKey": "a" }"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("AD-1~AD-3 펫 종: 등록 201(active=false 반영), 중복 409, rarity 변경 400, 움직임 매핑 · MOTION 아님 400")
    void adminSpecies() throws Exception {
        User admin = signUp(Role.ADMIN);
        String code = "bunny_" + UUID.randomUUID().toString().substring(0, 4);
        String body = """
                { "code": "%s", "name": "토끼", "rarity": "RARE", "baseAssetKey": "pets/bunny/base.png", "description": "깡총", "active": false }"""
                .formatted(code);
        var created = mvc.perform(post("/api/v1/admin/pet-species").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value(code))
                .andExpect(jsonPath("$.data.active").value(false))
                .andReturn();
        String speciesId = om.readTree(created.getResponse().getContentAsString()).path("data").path("id").asText();
        mvc.perform(post("/api/v1/admin/pet-species").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PET_SPECIES_CODE_DUPLICATED"));
        mvc.perform(patch("/api/v1/admin/pet-species/" + speciesId).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"rarity\": \"EPIC\" }"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/admin/pet-species/" + speciesId).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"active\": true, \"name\": \"아기 토끼\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.name").value("아기 토끼"))
                .andExpect(jsonPath("$.data.rarity").value("RARE"));

        UUID motion = itemService.getByCode("motion_idle").getId();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/admin/pet-species/" + speciesId + "/motions")
                        .header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"motions\": [ { \"affectionLevel\": 2, \"motionItemId\": \"%s\" } ] }".formatted(motion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.motions[0].affectionLevel").value(2));
        UUID rug = itemService.getByCode("fluffy_rug").getId();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/admin/pet-species/" + speciesId + "/motions")
                        .header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"motions\": [ { \"affectionLevel\": 3, \"motionItemId\": \"%s\" } ] }".formatted(rug)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ITEM_SLOT_MISMATCH"));
    }
}
