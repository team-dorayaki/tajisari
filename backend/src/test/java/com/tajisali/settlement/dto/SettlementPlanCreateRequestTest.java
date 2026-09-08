package com.tajisali.settlement.dto;

import com.tajisali.common.config.JacksonConfig;
import com.tajisali.common.response.ApiResponse;
import com.tajisali.settlement.domain.CostType;
import com.tajisali.settlement.domain.CurrencyCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JsonTest
@Import(JacksonConfig.class)
class SettlementPlanCreateRequestTest {

    @Autowired
    private JsonMapper mapper;

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void 정상_요청은_역직렬화되고_중첩_검증을_통과한다() throws Exception {
        var request = mapper.readValue(validRequestJson(), SettlementPlanCreateRequest.class);

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.getMoveInDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(request.getAdditionalInitialCosts()).hasSize(1);
        assertThat(request.getMonthlyLivingCosts()).hasSize(1);
    }

    @Test
    void 필수_필드가_누락되면_검증에_실패한다() {
        var request = new SettlementPlanCreateRequest();

        assertThat(propertiesOf(validator.validate(request))).contains(
                "moveInDate", "plannedStayMonths", "noIncomePeriodMonths", "availableFunds",
                "emergencyReserve", "additionalInitialCosts", "monthlyLivingCosts"
        );
    }

    @Test
    void 중첩_금액이_null이거나_음수이면_검증에_실패한다() {
        var request = validRequest();
        request.getAvailableFunds().setKrw(null);
        request.getEmergencyReserve().setJpy(-1L);

        assertThat(propertiesOf(validator.validate(request)))
                .contains("availableFunds.krw", "emergencyReserve.jpy");
    }

    @Test
    void 비용_목록의_null_원소는_검증에_실패한다() {
        var request = validRequest();
        request.setAdditionalInitialCosts(Collections.singletonList(null));

        assertThat(propertiesOf(validator.validate(request))).contains("additionalInitialCosts[0].<list element>");
    }

    @Test
    void 비용_항목의_누락된_종류와_통화는_검증에_실패한다() {
        var request = validRequest();
        request.setMonthlyLivingCosts(List.of(new SettlementPlanCostItemRequest(null, 1L, null)));

        assertThat(propertiesOf(validator.validate(request))).contains(
                "monthlyLivingCosts[0].type", "monthlyLivingCosts[0].currency"
        );
    }

    @Test
    void 무소득_예상기간_음수는_검증에_실패한다() {
        var request = validRequest();
        request.setNoIncomePeriodMonths(-1);

        assertThat(propertiesOf(validator.validate(request))).contains("noIncomePeriodMonths");
    }

    @Test
    void 비용_항목의_null_금액은_검증에_실패한다() {
        var request = validRequest();
        request.setMonthlyLivingCosts(
                List.of(new SettlementPlanCostItemRequest(CostType.FOOD, null, CurrencyCode.JPY)));

        assertThat(propertiesOf(validator.validate(request))).contains("monthlyLivingCosts[0].amount");
    }

    @Test
    void 체류기간_1개월은_허용한다() {
        var request = validRequest();
        request.setPlannedStayMonths(1);

        assertThat(propertiesOf(validator.validate(request))).doesNotContain("plannedStayMonths");
    }

    @Test
    void 체류기간_24개월은_허용한다() {
        var request = validRequest();
        request.setPlannedStayMonths(24);

        assertThat(propertiesOf(validator.validate(request))).doesNotContain("plannedStayMonths");
    }

    @Test
    void 체류기간_0개월은_거부한다() {
        var request = validRequest();
        request.setPlannedStayMonths(0);

        assertThat(propertiesOf(validator.validate(request))).contains("plannedStayMonths");
    }

    @Test
    void 체류기간_25개월은_거부한다() {
        var request = validRequest();
        request.setPlannedStayMonths(25);

        assertThat(propertiesOf(validator.validate(request))).contains("plannedStayMonths");
    }

    @Test
    void 금액_0원과_빈_비용_목록은_허용한다() {
        var request = validRequest();
        request.setAvailableFunds(new CurrencyAmountsRequest(0L, 0L));
        request.setEmergencyReserve(new CurrencyAmountsRequest(0L, 0L));
        request.setAdditionalInitialCosts(List.of());
        request.setMonthlyLivingCosts(List.of());

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 초기비용_목록이_5개를_초과하면_거부한다() {
        var request = validRequest();
        request.setAdditionalInitialCosts(List.of(item(), item(), item(), item(), item(), item()));

        assertThat(propertiesOf(validator.validate(request))).contains("additionalInitialCosts");
    }

    @Test
    void 월생활비_목록이_6개를_초과하면_거부한다() {
        var request = validRequest();
        request.setMonthlyLivingCosts(List.of(item(), item(), item(), item(), item(), item(), item()));

        assertThat(propertiesOf(validator.validate(request))).contains("monthlyLivingCosts");
    }

    @Test
    void 잘못된_날짜와_Enum은_역직렬화에_실패한다() {
        assertThatThrownBy(() -> mapper.readValue(
                validRequestJson().replace("2026-10-15", "not-a-date"), SettlementPlanCreateRequest.class
        )).isInstanceOf(DatabindException.class);
        assertThatThrownBy(() -> mapper.readValue(
                validRequestJson().replace("AIRFARE", "UNKNOWN"), SettlementPlanCreateRequest.class
        )).isInstanceOf(DatabindException.class);
    }

    @Test
    void 날짜_배열은_Jackson_기본_역직렬화로_허용한다() throws Exception {
        var request = mapper.readValue(
                validRequestJson().replace("\"2026-10-15\"", "[2026, 10, 15]"),
                SettlementPlanCreateRequest.class
        );

        assertThat(request.getMoveInDate()).isEqualTo(LocalDate.of(2026, 10, 15));
    }

    @Test
    void 숫자_문자열은_Jackson_기본_역직렬화로_허용한다() throws Exception {
        var request = mapper.readValue(
                validRequestJson()
                        .replace("\"plannedStayMonths\": 12", "\"plannedStayMonths\": \"12\"")
                        .replace("\"amount\": 40000", "\"amount\": \"40000\""),
                SettlementPlanCreateRequest.class
        );

        assertThat(request.getPlannedStayMonths()).isEqualTo(12);
        assertThat(request.getAdditionalInitialCosts().getFirst().getAmount()).isEqualTo(40_000L);
    }

    @Test
    void 소수_금액은_정수_금액으로_변환되지_않고_역직렬화에_실패한다() {
        assertThatThrownBy(() -> mapper.readValue(
                validRequestJson().replace("\"amount\": 40000", "\"amount\": 40000.5"),
                SettlementPlanCreateRequest.class
        )).isInstanceOf(DatabindException.class);
    }

    @Test
    void 소수_기간은_정수_기간으로_변환되지_않고_역직렬화에_실패한다() {
        assertThatThrownBy(() -> mapper.readValue(
                validRequestJson().replace("\"plannedStayMonths\": 12", "\"plannedStayMonths\": 12.5"),
                SettlementPlanCreateRequest.class
        )).isInstanceOf(DatabindException.class);
    }

    @Test
    void 숫자_Enum은_문자열_Enum으로_변환되지_않고_역직렬화에_실패한다() {
        assertThatThrownBy(() -> mapper.readValue(
                validRequestJson().replace("\"type\": \"AIRFARE\"", "\"type\": 0"),
                SettlementPlanCreateRequest.class
        )).isInstanceOf(DatabindException.class);
    }

    @Test
    void 숫자_통화는_문자열_Enum으로_변환되지_않고_역직렬화에_실패한다() {
        assertThatThrownBy(() -> mapper.readValue(
                validRequestJson().replace("\"currency\": \"JPY\"", "\"currency\": 0"),
                SettlementPlanCreateRequest.class
        )).isInstanceOf(DatabindException.class);
    }

    @Test
    void 성공_응답은_ApiResponse_안에서_계약_구조로_직렬화된다() {
        var response = new SettlementPlanCreateResponse(
                1L,
                new CurrencyTotalsResponse(0L, 62000L),
                new CurrencyTotalsResponse(0L, 115000L),
                "SAVED"
        );
        var json = mapper.valueToTree(ApiResponse.success(response));

        assertThat(json.path("success").asBoolean()).isTrue();
        assertThat(json.path("data").path("planId").asLong()).isEqualTo(1L);
        assertThat(json.path("data").path("initialCostTotals").path("jpy").asLong()).isEqualTo(62000L);
        assertThat(json.path("data").path("monthlyLivingCostTotals").path("jpy").asLong()).isEqualTo(115000L);
        assertThat(json.path("data").path("status").asString()).isEqualTo("SAVED");
        assertThat(json.path("error").isNull()).isTrue();
    }

    private List<String> propertiesOf(java.util.Set<ConstraintViolation<SettlementPlanCreateRequest>> violations) {
        return violations.stream().map(violation -> violation.getPropertyPath().toString()).toList();
    }

    private SettlementPlanCreateRequest validRequest() {
        return new SettlementPlanCreateRequest(
                LocalDate.of(2026, 10, 15),
                12,
                3,
                new CurrencyAmountsRequest(8_000_000L, 100_000L),
                new CurrencyAmountsRequest(1_000_000L, 0L),
                List.of(item()),
                List.of(new SettlementPlanCostItemRequest(CostType.FOOD, 40_000L, CurrencyCode.JPY))
        );
    }

    private SettlementPlanCostItemRequest item() {
        return new SettlementPlanCostItemRequest(CostType.AIRFARE, 40_000L, CurrencyCode.JPY);
    }

    private String validRequestJson() {
        return """
                {
                  "moveInDate": "2026-10-15",
                  "plannedStayMonths": 12,
                  "noIncomePeriodMonths": 3,
                  "availableFunds": {"krw": 8000000, "jpy": 100000},
                  "emergencyReserve": {"krw": 1000000, "jpy": 0},
                  "additionalInitialCosts": [
                    {"type": "AIRFARE", "amount": 40000, "currency": "JPY"}
                  ],
                  "monthlyLivingCosts": [
                    {"type": "FOOD", "amount": 40000, "currency": "JPY"}
                  ]
                }
                """;
    }
}
