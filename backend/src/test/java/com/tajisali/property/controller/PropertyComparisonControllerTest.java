package com.tajisali.property.controller;

import com.tajisali.common.config.JacksonConfig;
import com.tajisali.common.exception.GlobalExceptionHandler;
import com.tajisali.property.dto.PropertyComparisonRequest;
import com.tajisali.property.dto.PropertyComparisonResponse;
import com.tajisali.property.service.PropertyComparisonService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PropertyComparisonController.class)
@Import({JacksonConfig.class, GlobalExceptionHandler.class})
class PropertyComparisonControllerTest {

    private static final String USER_KEY = "00000000-0000-0000-0000-000000000001";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private PropertyComparisonService propertyComparisonService;

    @Test
    void 저장_매물_비교_결과를_반환한다() throws Exception {
        when(propertyComparisonService.compare(any(), eq(USER_KEY))).thenReturn(response());

        mockMvc.perform(post("/api/property-comparisons")
                        .cookie(new Cookie("tajisari_anonymous_user", USER_KEY))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "propertyIds": [11, 22] }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settlementPlanId").value(7))
                .andExpect(jsonPath("$.data.exchangeRate.krw").value(860))
                .andExpect(jsonPath("$.data.properties[0].propertyId").value(11))
                .andExpect(jsonPath("$.data.properties[0].costs.initialSettlementCost").value(320_000))
                .andExpect(jsonPath("$.data.properties[0].simulation.monthlyBalances[0].month").value(1))
                .andExpect(jsonPath("$.data.properties[0].highlights.lowestInitialSettlementCost").value(true))
                .andExpect(jsonPath("$.error").value((Object) null));

        verify(propertyComparisonService).compare(
                new PropertyComparisonRequest(List.of(11L, 22L)), USER_KEY);
    }

    @Test
    void 비교_대상이_두개_미만이면_Service를_호출하지_않는다() throws Exception {
        mockMvc.perform(post("/api/property-comparisons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "propertyIds": [11] }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));

        verifyNoInteractions(propertyComparisonService);
    }

    private PropertyComparisonResponse response() {
        return new PropertyComparisonResponse(
                7L,
                new PropertyComparisonResponse.ExchangeRate(100, 860),
                List.of(new PropertyComparisonResponse.PropertyComparison(
                        11L,
                        "신주쿠 원룸 A",
                        "properties/11/thumbnail.jpg",
                        1,
                        new PropertyComparisonResponse.Conditions(
                                "도쿄도", "신주쿠구", "신주쿠역", 8,
                                new BigDecimal("18.20"), 24),
                        new PropertyComparisonResponse.Costs(
                                300_000L, 80_000L, 320_000L,
                                80_000L, 220_000L, 960_000L,
                                0, false),
                        new PropertyComparisonResponse.Simulation(
                                true, 680_000L, 80_000L, 100_000L, 180_000L,
                                List.of(new PropertyComparisonResponse.MonthlyBalance(1, 500_000L)),
                                new BigDecimal("3.7"), false,
                                860_000L, 140_000L, 0L, 0L),
                        new PropertyComparisonResponse.Highlights(true, false, false))));
    }
}
