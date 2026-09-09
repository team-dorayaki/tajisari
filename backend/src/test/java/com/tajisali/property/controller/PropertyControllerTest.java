package com.tajisali.property.controller;

import com.tajisali.common.config.JacksonConfig;
import com.tajisali.common.exception.GlobalExceptionHandler;
import com.tajisali.property.dto.PropertyListResponse;
import com.tajisali.property.dto.PropertyDetailResponse;
import com.tajisali.property.dto.PropertyPriorityUpdateRequest;
import com.tajisali.property.dto.PropertyPriorityUpdateResponse;
import com.tajisali.property.service.PropertyCommandService;
import com.tajisali.property.service.PropertyQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.math.BigDecimal;
import jakarta.servlet.http.Cookie;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PropertyController.class)
@Import({JacksonConfig.class, GlobalExceptionHandler.class})
class PropertyControllerTest {

    private static final String USER_KEY = "00000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PropertyQueryService propertyQueryService;

    @MockitoBean
    private PropertyCommandService propertyCommandService;

    @Test
    void 매물_목록과_화면용_요약정보를_반환한다() throws Exception {
        var summary = new PropertyListResponse.PropertySummary(
                10L, "요코하마 스튜디오", 65_000L, 245_000L,
                new java.math.BigDecimal("3.2"), 1, "properties/10/thumbnail.jpg");
        when(propertyQueryService.getProperties(USER_KEY))
                .thenReturn(new PropertyListResponse(1, List.of(summary)));

        mockMvc.perform(get("/api/properties")
                        .cookie(new Cookie("tajisari_anonymous_user", USER_KEY)))
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

        verify(propertyQueryService).getProperties(USER_KEY);
    }

    @Test
    void 매물_ID로_매물_상세를_반환한다() throws Exception {
        var response = new PropertyDetailResponse(
                10L,
                7L,
                new PropertyDetailResponse.PropertyInfo(
                        "요코하마 스튜디오", "SUUMO", "https://suumo.jp/example",
                        "가나가와현", "요코하마시", new BigDecimal("25.40"),
                        "요코하마역", 8, null, 24, 1, 65_000L, 5_000L),
                List.of(new PropertyDetailResponse.PropertyImage(
                        1L, "https://cdn.example.com/room-1.jpg", 0)),
                new PropertyDetailResponse.CostAnalysis(
                        new PropertyDetailResponse.CostSummary(
                                245_000L, 227_000L, 70_000L,
                                227_000L, 18_000L,
                                65_000L, 180_000L, 33_000L),
                        new PropertyDetailResponse.CostGroups(
                                List.of(), List.of(), List.of(), List.of()),
                        List.of()),
                new PropertyDetailResponse.Simulation(
                        new PropertyDetailResponse.ExchangeRate(100, 860),
                        913_953L, 307_000L, true, 606_953L,
                        70_000L, 115_000L, 185_000L,
                        List.of(new PropertyDetailResponse.MonthlyBalance(1, 421_953L)),
                        new BigDecimal("3.2"), false, 12,
                        2_527_000L, 0L, 1_613_047L, 13_872_204L));
        when(propertyQueryService.getPropertyDetail(10L, USER_KEY)).thenReturn(response);

        mockMvc.perform(get("/api/properties/10")
                        .cookie(new Cookie("tajisari_anonymous_user", USER_KEY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.propertyId").value(10))
                .andExpect(jsonPath("$.data.images[0].imageUrl")
                        .value("https://cdn.example.com/room-1.jpg"))
                .andExpect(jsonPath("$.data.costAnalysis.summary.refundableAmount").value(65_000))
                .andExpect(jsonPath("$.data.costAnalysis.summary.minimumInitialCost").value(227_000))
                .andExpect(jsonPath("$.data.simulation.exchangeRate.krw").value(860))
                .andExpect(jsonPath("$.data.simulation.canMoveIn").value(true))
                .andExpect(jsonPath("$.data.simulation.monthlyBalances[0].month").value(1))
                .andExpect(jsonPath("$.data.simulation.livingMonths").value(3.2));

        verify(propertyQueryService).getPropertyDetail(10L, USER_KEY);
    }

    @Test
    void 매물_ID로_저장된_매물을_삭제한다() throws Exception {
        mockMvc.perform(delete("/api/properties/10")
                        .cookie(new Cookie("tajisari_anonymous_user", USER_KEY)))
                .andExpect(status().isNoContent());

        verify(propertyCommandService).deleteProperty(10L, USER_KEY);
    }

    @Test
    void 사용자의_매물_우선순위를_일괄_저장한다() throws Exception {
        var request = new PropertyPriorityUpdateRequest(10L, 20L);
        var response = new PropertyPriorityUpdateResponse(List.of(
                new PropertyPriorityUpdateResponse.Priority(10L, 1),
                new PropertyPriorityUpdateResponse.Priority(20L, 2)));
        when(propertyCommandService.updatePriorities(request, USER_KEY))
                .thenReturn(response);

        mockMvc.perform(put("/api/properties/priorities")
                        .cookie(new Cookie("tajisari_anonymous_user", USER_KEY))
                        .contentType("application/json")
                        .content("""
                                {
                                  "firstPriorityPropertyId": 10,
                                  "secondPriorityPropertyId": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.priorities[0].propertyId").value(10))
                .andExpect(jsonPath("$.data.priorities[0].priorityRank").value(1))
                .andExpect(jsonPath("$.data.priorities[1].propertyId").value(20))
                .andExpect(jsonPath("$.data.priorities[1].priorityRank").value(2))
                .andExpect(jsonPath("$.error").value((Object) null));

        verify(propertyCommandService).updatePriorities(request, USER_KEY);
    }
}
