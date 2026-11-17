package com.gorapaduk.makingpet.item.integration;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gorapaduk.makingpet.item.service.ItemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** I-1 카탈로그의 QueryDSL 동적 조건(ItemRepositoryImpl.searchActive). 시드 ACCESSORY 3개 기준. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ItemCatalogQueryTest {

    @Autowired MockMvc mvc;
    @Autowired ItemService itemService;

    @Test
    @DisplayName("category 를 주면 그 분류만, 이름 오름차순으로 나온다")
    void filterByCategory() throws Exception {
        mvc.perform(get("/api/v1/items").param("category", "ACCESSORY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.items[*].category", everyItem(is("ACCESSORY"))))
                .andExpect(jsonPath("$.data.items[*].code", contains("yellow_boots", "ribbon", "sprout_hat")));
    }

    @Test
    @DisplayName("같은 조건으로 페이지를 나누면 개수·마지막 페이지가 맞는다")
    void paging() throws Exception {
        mvc.perform(get("/api/v1/items").param("category", "ACCESSORY").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].code").value("sprout_hat"))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    @DisplayName("판매 중지(active=false)한 아이템은 목록과 개수에서 빠진다")
    void excludesInactive() throws Exception {
        var ribbon = itemService.getByCode("ribbon");
        itemService.update(ribbon.getId(), null, null, null, null, false);

        mvc.perform(get("/api/v1/items").param("category", "ACCESSORY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.items[*].code", contains("yellow_boots", "sprout_hat")));
    }
}
