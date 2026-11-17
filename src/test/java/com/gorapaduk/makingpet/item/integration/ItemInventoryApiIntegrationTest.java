package com.gorapaduk.makingpet.item.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.gorapaduk.makingpet.common.constants.ErrorCode;
import com.gorapaduk.makingpet.common.exception.BusinessException;
import com.gorapaduk.makingpet.item.domain.Item;
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
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ItemInventoryApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired ItemService itemService;
    @Autowired InventoryService inventoryService;

    private String registerAndLogin(String email, String nickname) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "password1234", "nickname": "%s" }""".formatted(email, nickname)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.wallet.balance").value(300));      // 가입 선물
        var login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "password1234" }""".formatted(email)))
                .andExpect(status().isOk()).andReturn();
        return om.readTree(login.getResponse().getContentAsString()).path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("I-1 아이템 카탈로그는 익명으로 조회되고 시드 아이템이 들어 있다")
    void catalogPublic() throws Exception {
        mvc.perform(get("/api/v1/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(7)));
        mvc.perform(get("/api/v1/items").param("category", "FURNITURE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].code").value("fluffy_rug"));
    }

    @Test
    @DisplayName("I-1 size 가 50 을 넘으면 400 PAGINATION_SIZE_TOO_LARGE")
    void pageSizeGuard() throws Exception {
        mvc.perform(get("/api/v1/items").param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PAGINATION_SIZE_TOO_LARGE"));
    }

    @Test
    @DisplayName("I-2 가입하면 인벤토리에 기본 테마 1개(마이 홈에 적용되어 reserve)가 있고, W-1 잔액은 300이다")
    void signupSideEffects() throws Exception {
        String token = registerAndLogin("inv@example.com", "인벤");
        mvc.perform(get("/api/v1/inventory").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].item.code").value(Item.DEFAULT_THEME_CODE))
                .andExpect(jsonPath("$.data.items[0].quantity").value(1))
                .andExpect(jsonPath("$.data.items[0].reserved").value(1))      // 마이 홈 테마로 장착 중
                .andExpect(jsonPath("$.data.items[0].available").value(0));
        mvc.perform(get("/api/v1/wallet").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(300));
        mvc.perform(get("/api/v1/wallet/transactions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].reason").value("SIGNUP_GIFT"))
                .andExpect(jsonPath("$.data.items[0].amount").value(300));
    }

    @Test
    @DisplayName("InventoryService: reserve 한 수량은 withdraw 할 수 없고, release 하면 다시 가능하다")
    void reserveBlocksWithdraw() {
        UUID memberId = UUID.randomUUID();
        UUID ribbon = itemService.getByCode("ribbon").getId();

        inventoryService.grant(memberId, ribbon, 2);
        inventoryService.reserve(memberId, ribbon, 2);                        // 둘 다 장착
        assertThat(inventoryService.available(memberId, ribbon)).isZero();

        assertThatThrownBy(() -> inventoryService.withdraw(memberId, ribbon, 1))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVENTORY_INSUFFICIENT);

        inventoryService.release(memberId, ribbon, 1);
        var row = inventoryService.withdraw(memberId, ribbon, 1);              // 경매 등록 에스크로
        assertThat(row.getQuantity()).isEqualTo(1);
        assertThat(row.getReserved()).isEqualTo(1);
        assertThat(row.getAvailable()).isZero();
    }

    @Test
    @DisplayName("InventoryService: 없는 아이템을 grant 하면 ITEM_NOT_FOUND")
    void grantUnknownItem() {
        assertThatThrownBy(() -> inventoryService.grant(UUID.randomUUID(), UUID.randomUUID(), 1))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ITEM_NOT_FOUND);
    }
}
