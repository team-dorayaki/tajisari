package com.tajisali.property.controller;

import com.tajisali.common.config.JacksonConfig;
import com.tajisali.common.exception.GlobalExceptionHandler;
import com.tajisali.property.dto.PropertyListResponse;
import com.tajisali.property.dto.PropertyDetailResponse;
import com.tajisali.property.service.PropertyQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PropertyController.class)
@Import({JacksonConfig.class, GlobalExceptionHandler.class})
class PropertyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PropertyQueryService propertyQueryService;

    @Test
    void 매물_목록과_화면용_요약정보를_반환한다() throws Exception {
        var summary = new PropertyListResponse.PropertySummary(
                10L, "요코하마 스튜디오", 65_000L, 245_000L,
                new java.math.BigDecimal("3.2"), 1, "properties/10/thumbnail.jpg");
        when(propertyQueryService.getProperties())
                .thenReturn(new PropertyListResponse(1, List.of(summary)));

        mockMvc.perform(get("/api/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.properties[0].propertyId").value(10))
                .andExpect(jsonPath("$.data.properties[0].propertyName").value("요코하마 스튜디오"))
                .andExpect(jsonPath("$.data.properties[0].rent").value(65_000))
                .andExpect(jsonPath("$.data.properties[0].initialCost").value(245_000))
                .andExpect(jsonPath("$.data.properties[0].livingMonths").value(3.2))
                .andExpect(jsonPath("$.data.properties[0].priorityRank").value(1))
                .andExpect(jsonPath("$.data.properties[0].thumbnailUrl")
                        .value("properties/10/thumbnail.jpg"))
                .andExpect(jsonPath("$.error").value((Object) null));

        verify(propertyQueryService).getProperties();
    }

    @Test
    void 매물_ID로_매물_상세를_반환한다() throws Exception {
        var response = new PropertyDetailResponse(
                10L,
                7L,
                new PropertyDetailResponse.PropertyInfo(
                        "요코하마 스튜디오", "SUUMO", "https://suumo.jp/example",
                        "가나가와현", "요코하마시", new BigDecimal("25.40"),
                        "요코하마역", 8, null, 24, 1),
                List.of("https://cdn.example.com/room-1.jpg"),
                new PropertyDetailResponse.CostAnalysis(
                        65_000L, 5_000L, 65_000L, 0L,
                        245_000L, 70_000L,
                        65_000L, 180_000L, List.of()),
                new PropertyDetailResponse.Simulation(
                        new PropertyDetailResponse.ExchangeRate(100, 860),
                        913_953L, 307_000L, 606_953L,
                        70_000L, 115_000L, 185_000L,
                        new BigDecimal("3.2"), 12, -1_613_047L, 1_613_047L));
        when(propertyQueryService.getPropertyDetail(10L)).thenReturn(response);

        mockMvc.perform(get("/api/properties/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.propertyId").value(10))
                .andExpect(jsonPath("$.data.imageUrls[0]")
                        .value("https://cdn.example.com/room-1.jpg"))
                .andExpect(jsonPath("$.data.costAnalysis.refundableAmount").value(65_000))
                .andExpect(jsonPath("$.data.simulation.exchangeRate.krw").value(860))
                .andExpect(jsonPath("$.data.simulation.livingMonths").value(3.2));

        verify(propertyQueryService).getPropertyDetail(10L);
    }
}
