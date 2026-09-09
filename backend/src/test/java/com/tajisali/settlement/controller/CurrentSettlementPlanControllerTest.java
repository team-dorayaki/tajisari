package com.tajisali.settlement.controller;

import com.tajisali.common.config.JacksonConfig;
import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.common.exception.GlobalExceptionHandler;
import com.tajisali.settlement.domain.MonthlyLivingCostInputMethod;
import com.tajisali.settlement.dto.CurrencyTotalsResponse;
import com.tajisali.settlement.dto.SettlementPlanCreateRequest;
import com.tajisali.settlement.dto.SettlementPlanResponse;
import com.tajisali.settlement.service.SettlementPlanService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CurrentSettlementPlanController.class)
@Import({JacksonConfig.class, GlobalExceptionHandler.class})
class CurrentSettlementPlanControllerTest {

    private static final String ANONYMOUS_USER_COOKIE = "tajisari_anonymous_user";
    private static final String USER_KEY = "00000000-0000-0000-0000-000000000701";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SettlementPlanService settlementPlanService;

    @Test
    void GET은_Cookie의_현재_사용자_계획을_반환한다() throws Exception {
        when(settlementPlanService.getCurrent(USER_KEY)).thenReturn(planResponse());

        mockMvc.perform(get("/api/me/settlement-plan")
                        .cookie(new Cookie(ANONYMOUS_USER_COOKIE, USER_KEY)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.planId").value(42))
                .andExpect(jsonPath("$.data.preparedFunds.krw").value(8_000_000));

        verify(settlementPlanService).getCurrent(USER_KEY);
    }

    @Test
    void GET은_Cookie가_없으면_404이고_Cookie를_발급하지_않는다() throws Exception {
        when(settlementPlanService.getCurrent(isNull()))
                .thenThrow(new BusinessException(ErrorCode.SETTLEMENT_PLAN_NOT_FOUND));

        mockMvc.perform(get("/api/me/settlement-plan"))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.error.code").value("SETTLEMENT_PLAN_NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not-a-uuid",
            "00000000-0000-0000-0000-000000000799"
    })
    void GET은_malformed_또는_stale_Cookie면_404이고_Cookie를_발급하지_않는다(
            String userKey) throws Exception {
        when(settlementPlanService.getCurrent(userKey))
                .thenThrow(new BusinessException(ErrorCode.SETTLEMENT_PLAN_NOT_FOUND));

        mockMvc.perform(get("/api/me/settlement-plan")
                        .cookie(new Cookie(ANONYMOUS_USER_COOKIE, userKey)))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.error.code").value("SETTLEMENT_PLAN_NOT_FOUND"));
    }

    @Test
    void PUT은_Cookie와_기존_요청_DTO를_Service에_전달한다() throws Exception {
        when(settlementPlanService.updateCurrent(eq(USER_KEY), any()))
                .thenReturn(planResponse());

        mockMvc.perform(put("/api/me/settlement-plan")
                        .cookie(new Cookie(ANONYMOUS_USER_COOKIE, USER_KEY))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.planId").value(42));

        ArgumentCaptor<SettlementPlanCreateRequest> requestCaptor =
                ArgumentCaptor.forClass(SettlementPlanCreateRequest.class);
        verify(settlementPlanService).updateCurrent(eq(USER_KEY), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getMoveInDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(requestCaptor.getValue().getPreparedFunds().getKrw()).isEqualTo(8_000_000L);
    }

    @Test
    void PUT은_Cookie가_없으면_404이고_Cookie를_발급하지_않는다() throws Exception {
        when(settlementPlanService.updateCurrent(isNull(), any()))
                .thenThrow(new BusinessException(ErrorCode.SETTLEMENT_PLAN_NOT_FOUND));

        mockMvc.perform(put("/api/me/settlement-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.error.code").value("SETTLEMENT_PLAN_NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not-a-uuid",
            "00000000-0000-0000-0000-000000000799"
    })
    void PUT은_malformed_또는_stale_Cookie면_404이고_Cookie를_발급하지_않는다(
            String userKey) throws Exception {
        when(settlementPlanService.updateCurrent(eq(userKey), any()))
                .thenThrow(new BusinessException(ErrorCode.SETTLEMENT_PLAN_NOT_FOUND));

        mockMvc.perform(put("/api/me/settlement-plan")
                        .cookie(new Cookie(ANONYMOUS_USER_COOKIE, userKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.error.code").value("SETTLEMENT_PLAN_NOT_FOUND"));
    }

    @Test
    void PUT의_DTO_검증이_실패하면_400이고_Service와_Cookie를_사용하지_않는다() throws Exception {
        String invalidRequest = validRequestJson().replace(
                "\"plannedStayMonths\": 12",
                "\"plannedStayMonths\": 25");

        mockMvc.perform(put("/api/me/settlement-plan")
                        .cookie(new Cookie(ANONYMOUS_USER_COOKIE, USER_KEY))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));

        verifyNoInteractions(settlementPlanService);
    }

    private SettlementPlanResponse planResponse() {
        return new SettlementPlanResponse(
                42L,
                LocalDate.of(2026, 10, 15),
                12,
                new CurrencyTotalsResponse(8_000_000L, 100_000L),
                new CurrencyTotalsResponse(1_000_000L, 0L),
                List.of(),
                List.of(),
                MonthlyLivingCostInputMethod.DEFAULT,
                new CurrencyTotalsResponse(0L, 0L),
                new CurrencyTotalsResponse(0L, 0L));
    }

    private static String validRequestJson() {
        return """
                {
                  "moveInDate": "2026-10-15",
                  "plannedStayMonths": 12,
                  "preparedFunds": {"krw": 8000000, "jpy": 100000},
                  "emergencyReserve": {"krw": 1000000, "jpy": 0},
                  "additionalInitialCosts": [],
                  "monthlyLivingCosts": [],
                  "monthlyLivingCostInputMethod": "DEFAULT"
                }
                """;
    }
}
