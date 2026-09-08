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
import com.tajisali.settlement.dto.SettlementPlanResponse;
import com.tajisali.settlement.service.SettlementPlanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SettlementPlanController.class)
@Import({JacksonConfig.class, GlobalExceptionHandler.class})
class SettlementPlanControllerTest {

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

        when(settlementPlanService.create(any())).thenReturn(serviceResponse);

        // when
        mockMvc.perform(post("/api/settlement-plans")
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
                .andExpect(jsonPath("$.error").value((Object) null));

        var requestCaptor = ArgumentCaptor.forClass(SettlementPlanCreateRequest.class);

        verify(settlementPlanService).create(requestCaptor.capture());

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
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));

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
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_REQUEST"));

        verifyNoInteractions(settlementPlanService);
    }

    @Test
    void Service에서_BusinessException이_발생하면_계약된_오류_응답을_반환한다() throws Exception {
        // given
        when(settlementPlanService.create(any()))
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
    void 예상하지_못한_Service_예외가_발생하면_내부_정보를_노출하지_않고_500을_반환한다() throws Exception {
        // given
        when(settlementPlanService.create(any()))
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

    @Test
    void 조회는_계획_ID를_전달하고_전체_조회_응답을_반환한다() throws Exception {
        when(settlementPlanService.get(42L)).thenReturn(planResponse());

        mockMvc.perform(get("/api/settlement-plans/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.planId").value(42))
                .andExpect(jsonPath("$.data.moveInDate").value("2026-10-15"))
                .andExpect(jsonPath("$.data.plannedStayMonths").value(12))
                .andExpect(jsonPath("$.data.preparedFunds.krw").value(8_000_000))
                .andExpect(jsonPath("$.data.preparedFunds.jpy").value(100_000))
                .andExpect(jsonPath("$.data.emergencyReserve.krw").value(1_000_000))
                .andExpect(jsonPath("$.data.emergencyReserve.jpy").value(0))
                .andExpect(jsonPath("$.data.additionalInitialCosts[0].type").value("AIRFARE"))
                .andExpect(jsonPath("$.data.additionalInitialCosts[0].amount").value(40_000))
                .andExpect(jsonPath("$.data.additionalInitialCosts[0].currency").value("JPY"))
                .andExpect(jsonPath("$.data.monthlyLivingCosts[0].type").value("FOOD"))
                .andExpect(jsonPath("$.data.monthlyLivingCostInputMethod").value("DEFAULT"))
                .andExpect(jsonPath("$.data.additionalInitialCostTotals.krw").value(0))
                .andExpect(jsonPath("$.data.additionalInitialCostTotals.jpy").value(40_000))
                .andExpect(jsonPath("$.data.monthlyLivingCostTotals.krw").value(0))
                .andExpect(jsonPath("$.data.monthlyLivingCostTotals.jpy").value(40_000))
                .andExpect(jsonPath("$.error").value((Object) null));

        verify(settlementPlanService).get(42L);
    }

    @Test
    void 수정은_계획_ID와_요청을_전달하고_200_응답을_반환한다() throws Exception {
        when(settlementPlanService.update(eq(42L), any())).thenReturn(planResponse());

        mockMvc.perform(put("/api/settlement-plans/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.planId").value(42))
                .andExpect(jsonPath("$.data.moveInDate").value("2026-10-15"))
                .andExpect(jsonPath("$.data.preparedFunds.krw").value(8_000_000))
                .andExpect(jsonPath("$.data.monthlyLivingCostInputMethod").value("DEFAULT"))
                .andExpect(jsonPath("$.data.additionalInitialCosts[0].type").value("AIRFARE"))
                .andExpect(jsonPath("$.data.monthlyLivingCosts[0].amount").value(40_000))
                .andExpect(jsonPath("$.data.additionalInitialCostTotals.jpy").value(40_000))
                .andExpect(jsonPath("$.data.monthlyLivingCostTotals.jpy").value(40_000))
                .andExpect(jsonPath("$.error").value((Object) null));

        var captor = ArgumentCaptor.forClass(SettlementPlanCreateRequest.class);
        verify(settlementPlanService).update(eq(42L), captor.capture());
        var request = captor.getValue();
        assertThat(request.getMoveInDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(request.getPlannedStayMonths()).isEqualTo(12);
        assertThat(request.getPreparedFunds().getKrw()).isEqualTo(8_000_000L);
        assertThat(request.getPreparedFunds().getJpy()).isEqualTo(100_000L);
        assertThat(request.getEmergencyReserve().getKrw()).isEqualTo(1_000_000L);
        assertThat(request.getEmergencyReserve().getJpy()).isZero();
        assertThat(request.getAdditionalInitialCosts().getFirst().getType()).isEqualTo(CostType.AIRFARE);
        assertThat(request.getAdditionalInitialCosts().getFirst().getAmount()).isEqualTo(40_000L);
        assertThat(request.getAdditionalInitialCosts().getFirst().getCurrency()).isEqualTo(CurrencyCode.JPY);
        assertThat(request.getMonthlyLivingCosts().getFirst().getType()).isEqualTo(CostType.FOOD);
        assertThat(request.getMonthlyLivingCostInputMethod()).isEqualTo(MonthlyLivingCostInputMethod.DEFAULT);
        verify(settlementPlanService, never()).create(any());
    }

    @ParameterizedTest
    @MethodSource("invalidInputBodies")
    void 수정_입력_검증_실패는_공통_입력_오류를_반환한다(String body) throws Exception {
        mockMvc.perform(put("/api/settlement-plans/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));

        verifyNoInteractions(settlementPlanService);
    }

    @ParameterizedTest
    @MethodSource("invalidRequestBodies")
    void 수정_역직렬화_실패는_공통_요청_형식_오류를_반환한다(String body) throws Exception {
        mockMvc.perform(put("/api/settlement-plans/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_REQUEST"));

        verifyNoInteractions(settlementPlanService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT"})
    void 없는_계획의_조회와_수정은_공통_404를_반환한다(String method) throws Exception {
        var exception = new BusinessException(ErrorCode.SETTLEMENT_PLAN_NOT_FOUND);
        var request = method.equals("GET") ? get("/api/settlement-plans/42") : put("/api/settlement-plans/42");
        if (method.equals("GET")) {
            when(settlementPlanService.get(42L)).thenThrow(exception);
        } else {
            when(settlementPlanService.update(eq(42L), any())).thenThrow(exception);
        }

        mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(validRequestJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value((Object) null))
                .andExpect(jsonPath("$.error.code").value("SETTLEMENT_PLAN_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value(ErrorCode.SETTLEMENT_PLAN_NOT_FOUND.getMessage()));
    }

    @ParameterizedTest
    @CsvSource({
            "0, COMMON_INVALID_INPUT",
            "-1, COMMON_INVALID_INPUT",
            "invalid, COMMON_INVALID_REQUEST",
            "9223372036854775808, COMMON_INVALID_REQUEST"
    })
    void 유효하지_않은_경로_ID는_조회와_수정에서_거부한다(String planId, String code) throws Exception {
        for (var request : List.of(get("/api/settlement-plans/" + planId), put("/api/settlement-plans/" + planId))) {
            mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(validRequestJson()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data").value((Object) null))
                    .andExpect(jsonPath("$.error.code").value(code));
        }

        verifyNoInteractions(settlementPlanService);
    }

    @Test
    void 수정의_Service_비즈니스_예외는_공통_오류로_연결한다() throws Exception {
        when(settlementPlanService.update(eq(42L), any()))
                .thenThrow(new BusinessException(ErrorCode.SETTLEMENT_PLAN_RESERVE_EXCEEDS_FUNDS));

        mockMvc.perform(put("/api/settlement-plans/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("SETTLEMENT_PLAN_RESERVE_EXCEEDS_FUNDS"));
    }

    @Test
    void 수정의_예상하지_못한_예외는_내부_정보를_노출하지_않는다() throws Exception {
        when(settlementPlanService.update(eq(42L), any()))
                .thenThrow(new IllegalStateException("SELECT * FROM private_table"));

        var result = mockMvc.perform(put("/api/settlement-plans/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value((Object) null))
                .andExpect(jsonPath("$.error.code").value("COMMON_INTERNAL_ERROR"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("SELECT", "private_table", "IllegalStateException", "stackTrace");
    }

    private SettlementPlanResponse planResponse() {
        return new SettlementPlanResponse(
                42L, LocalDate.of(2026, 10, 15), 12,
                new CurrencyTotalsResponse(8_000_000L, 100_000L),
                new CurrencyTotalsResponse(1_000_000L, 0L),
                List.of(new SettlementPlanResponse.CostItem(CostType.AIRFARE, 40_000L, CurrencyCode.JPY)),
                List.of(new SettlementPlanResponse.CostItem(CostType.FOOD, 40_000L, CurrencyCode.JPY)),
                MonthlyLivingCostInputMethod.DEFAULT,
                new CurrencyTotalsResponse(0L, 40_000L),
                new CurrencyTotalsResponse(0L, 40_000L));
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
