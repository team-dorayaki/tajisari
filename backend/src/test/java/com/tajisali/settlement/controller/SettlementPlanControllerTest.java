package com.tajisali.settlement.controller;

import com.tajisali.common.config.JacksonConfig;
import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.common.exception.GlobalExceptionHandler;
import com.tajisali.settlement.domain.CostType;
import com.tajisali.settlement.domain.CurrencyCode;
import com.tajisali.settlement.domain.MonthlyLivingCostInputMethod;
import com.tajisali.settlement.dto.CurrencyTotalsResponse;
import com.tajisali.settlement.dto.SettlementPlanCreateRequest;
import com.tajisali.settlement.dto.SettlementPlanCreateResponse;
import com.tajisali.settlement.service.SettlementPlanCreateResult;
import com.tajisali.settlement.service.SettlementPlanService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SettlementPlanController.class)
@Import({JacksonConfig.class, GlobalExceptionHandler.class})
class SettlementPlanControllerTest {

    private static final String ANONYMOUS_USER_COOKIE = "tajisari_anonymous_user";
    private static final String USER_KEY = "00000000-0000-0000-0000-000000000401";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SettlementPlanService settlementPlanService;

    @ParameterizedTest
    @ValueSource(strings = {"DIRECT", "DEFAULT"})
    void 정상_요청은_Service에_전달되고_생성_응답을_반환한다(String inputMethod) throws Exception {
        // given
        var serviceResponse = new SettlementPlanCreateResponse(
                1L,
                new CurrencyTotalsResponse(0L, 62_000L),
                new CurrencyTotalsResponse(0L, 115_000L),
                "SAVED");

        when(settlementPlanService.create(any(), isNull()))
                .thenReturn(new SettlementPlanCreateResult(serviceResponse, USER_KEY));

        // when
        MvcResult result = mockMvc.perform(post("/api/settlement-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson().replace("\"DEFAULT\"", "\"" + inputMethod + "\"")))

                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.planId").value(1))
                .andExpect(jsonPath("$.data.additionalInitialCostTotals.krw").value(0))
                .andExpect(jsonPath("$.data.additionalInitialCostTotals.jpy").value(62_000))
                .andExpect(jsonPath("$.data.monthlyLivingCostTotals.krw").value(0))
                .andExpect(jsonPath("$.data.monthlyLivingCostTotals.jpy").value(115_000))
                .andExpect(jsonPath("$.data.status").value("SAVED"))
                .andExpect(jsonPath("$.data.userKey").doesNotExist())
                .andExpect(jsonPath("$.error").value((Object) null))
                .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains(
                        ANONYMOUS_USER_COOKIE + "=" + USER_KEY,
                        "Path=/",
                        "Max-Age=31536000",
                        "HttpOnly",
                        "SameSite=Lax");

        var requestCaptor = ArgumentCaptor.forClass(SettlementPlanCreateRequest.class);

        verify(settlementPlanService).create(requestCaptor.capture(), isNull());

        var request = requestCaptor.getValue();

        assertThat(request.getMoveInDate())
                .isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(request.getPreparedFunds().getKrw())
                .isEqualTo(8_000_000L);
        assertThat(request.getMonthlyLivingCostInputMethod())
                .isEqualTo(MonthlyLivingCostInputMethod.valueOf(inputMethod));
        assertThat(request.getAdditionalInitialCosts().getFirst().getAmount())
                .isEqualTo(40_000L);
        assertThat(request.getAdditionalInitialCosts().getFirst().getCurrency())
                .isEqualTo(CurrencyCode.JPY);
        assertThat(request.getMonthlyLivingCosts().getFirst().getType())
                .isEqualTo(CostType.FOOD);
    }

    @Test
    void 기존_사용자_Cookie는_재발급하지_않는다() throws Exception {
        when(settlementPlanService.create(any(), eq(USER_KEY)))
                .thenReturn(createResult(USER_KEY));

        mockMvc.perform(post("/api/settlement-plans")
                        .cookie(new Cookie(ANONYMOUS_USER_COOKIE, USER_KEY))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.data.planId").value(1))
                .andExpect(jsonPath("$.data.userKey").doesNotExist());

        verify(settlementPlanService).create(any(), eq(USER_KEY));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "00000000-0000-0000-0000-000000000499",
            "not-a-uuid"
    })
    void stale_또는_잘못된_Cookie는_새_Cookie로_교체한다(String cookieValue) throws Exception {
        when(settlementPlanService.create(any(), eq(cookieValue)))
                .thenReturn(createResult(USER_KEY));

        MvcResult result = mockMvc.perform(post("/api/settlement-plans")
                        .cookie(new Cookie(ANONYMOUS_USER_COOKIE, cookieValue))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userKey").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains(ANONYMOUS_USER_COOKIE + "=" + USER_KEY, "HttpOnly", "SameSite=Lax");
    }

    @ParameterizedTest
    @MethodSource("invalidInputBodies")
    void DTO_입력_검증에_실패하면_서비스를_호출하지_않는다(String body) throws Exception {
        // given

        // when
        mockMvc.perform(post("/api/settlement-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value((Object) null))
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        verifyNoInteractions(settlementPlanService);
    }

    @ParameterizedTest
    @MethodSource("invalidRequestBodies")
    void JSON_역직렬화에_실패하면_서비스를_호출하지_않는다(String body) throws Exception {
        // given

        // when
        mockMvc.perform(post("/api/settlement-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value((Object) null))
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_REQUEST"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        verifyNoInteractions(settlementPlanService);
    }

    @Test
    void Service에서_BusinessException이_발생하면_계약된_오류_응답을_반환한다() throws Exception {
        // given
        when(settlementPlanService.create(any(), isNull()))
                .thenThrow(new BusinessException(
                        ErrorCode.SETTLEMENT_PLAN_RESERVE_EXCEEDS_FUNDS));

        // when
        mockMvc.perform(post("/api/settlement-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value((Object) null))
                .andExpect(jsonPath("$.error.code")
                        .value("SETTLEMENT_PLAN_RESERVE_EXCEEDS_FUNDS"));
    }

    @Test
    void 같은_사용자의_중복_계획은_409를_반환한다() throws Exception {
        when(settlementPlanService.create(any(), eq(USER_KEY)))
                .thenThrow(new BusinessException(ErrorCode.SETTLEMENT_PLAN_ALREADY_EXISTS));

        mockMvc.perform(post("/api/settlement-plans")
                        .cookie(new Cookie(ANONYMOUS_USER_COOKIE, USER_KEY))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isConflict())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SETTLEMENT_PLAN_ALREADY_EXISTS"));
    }

    @Test
    void 예상하지_못한_Service_예외가_발생하면_내부_정보를_노출하지_않고_500을_반환한다() throws Exception {
        // given
        when(settlementPlanService.create(any(), isNull()))
                .thenThrow(new IllegalStateException(
                        "SELECT * FROM private_table"));

        // when
        MvcResult result = mockMvc.perform(post("/api/settlement-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))

                // then
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value((Object) null))
                .andExpect(jsonPath("$.error.code")
                        .value("COMMON_INTERNAL_ERROR"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(
                        "SELECT",
                        "private_table",
                        "IllegalStateException");
    }

    private SettlementPlanCreateResult createResult(String userKey) {
        return new SettlementPlanCreateResult(
                new SettlementPlanCreateResponse(
                        1L,
                        new CurrencyTotalsResponse(0L, 62_000L),
                        new CurrencyTotalsResponse(0L, 115_000L),
                        "SAVED"),
                userKey);
    }

    private static Stream<String> invalidInputBodies() {
        return Stream.of(
                validRequestJson().replace(
                        "\"preparedFunds\": {\"krw\": 8000000, \"jpy\": 100000}",
                        "\"preparedFunds\": null"),
                validRequestJson().replace(
                        "\"plannedStayMonths\": 12",
                        "\"plannedStayMonths\": 25"),
                validRequestJson().replace(
                        "\"amount\": 40000",
                        "\"amount\": -1"),
                validRequestJson().replace(
                        "{\"type\": \"AIRFARE\", \"amount\": 40000, \"currency\": \"JPY\"}",
                        "null"),
                validRequestJson().replace(
                        ",\n  \"monthlyLivingCostInputMethod\": \"DEFAULT\"",
                        ""),
                validRequestJson().replace(
                        "\"monthlyLivingCostInputMethod\": \"DEFAULT\"",
                        "\"monthlyLivingCostInputMethod\": null"));
    }

    private static Stream<String> invalidRequestBodies() {
        return Stream.of(
                "{",
                validRequestJson().replace(
                        "2026-10-15",
                        "not-a-date"),
                validRequestJson().replace(
                        "\"AIRFARE\"",
                        "\"UNKNOWN\""),
                validRequestJson().replace(
                        "\"amount\": 40000",
                        "\"amount\": 40000.5"),
                validRequestJson().replace(
                        "\"plannedStayMonths\": 12",
                        "\"plannedStayMonths\": 12.5"),
                validRequestJson().replace(
                        "\"type\": \"AIRFARE\"",
                        "\"type\": 0"),
                validRequestJson().replace(
                        "\"monthlyLivingCostInputMethod\": \"DEFAULT\"",
                        "\"monthlyLivingCostInputMethod\": \"UNKNOWN\""));
    }

    private static String validRequestJson() {
        return """
                {
                  "moveInDate": "2026-10-15",
                  "plannedStayMonths": 12,
                  "preparedFunds": {"krw": 8000000, "jpy": 100000},
                  "emergencyReserve": {"krw": 1000000, "jpy": 0},
                  "additionalInitialCosts": [
                    {"type": "AIRFARE", "amount": 40000, "currency": "JPY"}
                  ],
                  "monthlyLivingCosts": [
                    {"type": "FOOD", "amount": 40000, "currency": "JPY"}
                  ],
                  "monthlyLivingCostInputMethod": "DEFAULT"
                }
                """;
    }
}
