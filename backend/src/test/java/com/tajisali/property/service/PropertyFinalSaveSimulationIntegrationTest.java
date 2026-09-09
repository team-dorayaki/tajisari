package com.tajisali.property.service;

import com.tajisali.exchange.service.ExchangeRateService;
import com.tajisali.property.domain.CostTiming;
import com.tajisali.property.domain.ObligationStatus;
import com.tajisali.property.dto.PropertyAnalysisResponse;
import com.tajisali.property.dto.PropertyConfirmRequest;
import com.tajisali.settlement.domain.CostCategory;
import com.tajisali.settlement.domain.CostType;
import com.tajisali.settlement.domain.CurrencyCode;
import com.tajisali.settlement.domain.MonthlyLivingCostInputMethod;
import com.tajisali.settlement.domain.SettlementPlan;
import com.tajisali.settlement.domain.SettlementPlanCostItem;
import com.tajisali.settlement.repository.SettlementPlanRepository;
import com.tajisali.user.repository.UserRepository;
import com.tajisali.user.service.AnonymousUserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        PropertyCommandService.class,
        PropertyQueryService.class,
        PropertyCostCalculationService.class,
        PropertyImageStorageService.class,
        PropertyFundSimulationService.class,
        ExchangeRateService.class,
        AnonymousUserService.class
})
class PropertyFinalSaveSimulationIntegrationTest {

    @Autowired
    private PropertyCommandService propertyCommandService;

    @Autowired
    private PropertyQueryService propertyQueryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SettlementPlanRepository settlementPlanRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 최종_저장한_매물의_확정_비용으로_상세_비용분석과_자금_시뮬레이션을_조회한다() {
        PropertyConfirmResult confirmResult = propertyCommandService.confirmUrl(
                confirmRequest(), null);
        saveDirectSettlementPlan(confirmResult.userKey());
        entityManager.flush();
        entityManager.clear();

        var detail = propertyQueryService.getPropertyDetail(
                confirmResult.response().propertyId(), confirmResult.userKey());

        assertThat(detail.costAnalysis().summary().initialCost()).isEqualTo(120_000L);
        assertThat(detail.costAnalysis().summary().monthlyCost()).isEqualTo(85_000L);

        var simulation = detail.simulation();
        assertThat(simulation).isNotNull();
        assertThat(simulation.availableFunds()).isEqualTo(400_000L);
        assertThat(simulation.initialCost()).isEqualTo(130_000L);
        assertThat(simulation.canMoveIn()).isTrue();
        assertThat(simulation.balanceAfterMoveIn()).isEqualTo(270_000L);
        assertThat(simulation.monthlyHousingCost()).isEqualTo(85_000L);
        assertThat(simulation.monthlyLivingCost()).isEqualTo(20_000L);
        assertThat(simulation.totalMonthlyCost()).isEqualTo(105_000L);
        assertThat(simulation.monthlyBalances())
                .extracting(balance -> balance.month(), balance -> balance.balance())
                .containsExactly(
                        tuple(1, 165_000L),
                        tuple(2, 60_000L),
                        tuple(3, -45_000L));
        assertThat(simulation.livingMonths()).isEqualByComparingTo(new BigDecimal("2.5"));
        assertThat(simulation.requiredFunds()).isEqualTo(445_000L);
        assertThat(simulation.surplus()).isZero();
        assertThat(simulation.shortageJpy()).isEqualTo(45_000L);
        assertThat(simulation.shortageKrw()).isEqualTo(387_000L);
    }

    @Test
    void DEFAULT_정착_계획이면_확정_비용이_있어도_시뮬레이션을_반환하지_않는다() {
        PropertyConfirmResult confirmResult = propertyCommandService.confirmUrl(
                confirmRequest(), null);
        saveSettlementPlan(confirmResult.userKey(), MonthlyLivingCostInputMethod.DEFAULT);
        entityManager.flush();
        entityManager.clear();

        var detail = propertyQueryService.getPropertyDetail(
                confirmResult.response().propertyId(), confirmResult.userKey());

        assertThat(detail.costAnalysis().summary().initialCost()).isEqualTo(120_000L);
        assertThat(detail.costAnalysis().summary().monthlyCost()).isEqualTo(85_000L);
        assertThat(detail.simulation()).isNull();
    }

    @Test
    void 금액_미확인_비용은_확정_비용에서_제외하고_상세_조회에_미확인으로_표시한다() {
        PropertyConfirmResult confirmResult = propertyCommandService.confirmUrl(
                confirmRequestWithUnknownInitialCost(), null);
        saveDirectSettlementPlan(confirmResult.userKey());
        entityManager.flush();
        entityManager.clear();

        var detail = propertyQueryService.getPropertyDetail(
                confirmResult.response().propertyId(), confirmResult.userKey());

        assertThat(detail.costAnalysis().summary().initialCost()).isEqualTo(120_000L);
        assertThat(detail.costAnalysis().excludedCosts())
                .extracting(excludedCost -> excludedCost.label(), excludedCost -> excludedCost.reason())
                .containsExactly(tuple("화재 보험료", "금액 미확인"));
        assertThat(detail.simulation().initialCost()).isEqualTo(130_000L);
    }

    private void saveDirectSettlementPlan(String userKey) {
        saveSettlementPlan(userKey, MonthlyLivingCostInputMethod.DIRECT);
    }

    private void saveSettlementPlan(String userKey, MonthlyLivingCostInputMethod inputMethod) {
        var user = userRepository.findByUserKey(userKey).orElseThrow();
        SettlementPlan plan = new SettlementPlan(
                user,
                LocalDate.of(2026, 10, 1),
                3,
                0L,
                500_000L,
                0L,
                100_000L,
                inputMethod);
        plan.addCostItem(new SettlementPlanCostItem(
                CostCategory.INITIAL, CostType.VISA_ADMINISTRATION, 10_000L, CurrencyCode.JPY));
        plan.addCostItem(new SettlementPlanCostItem(
                CostCategory.MONTHLY, CostType.FOOD, 20_000L, CurrencyCode.JPY));
        settlementPlanRepository.save(plan);
    }

    private PropertyConfirmRequest confirmRequest() {
        var rawResult = tools.jackson.databind.json.JsonMapper.builder().build().createObjectNode();
        rawResult.putObject("property").put("source", "integration-test");

        return new PropertyConfirmRequest(
                PropertyAnalysisResponse.SourceType.URL,
                "gemini-test",
                new PropertyConfirmRequest.PropertyInfo(
                        PropertyAnalysisResponse.SourceSite.SUUMO,
                        "https://suumo.jp/chintai/integration-test",
                        "통합 검증 매물",
                        "도쿄도", "신주쿠구", null, "신주쿠역", 8,
                        70_000L, 5_000L, 100_000L, 0L,
                        LocalDate.of(2026, 10, 1), 24, null),
                List.of(
                        new PropertyConfirmRequest.PropertyCostItem(
                                "계약 사무 수수료", "계약 사무 수수료", 20_000L, null,
                                ObligationStatus.REQUIRED, true, CostTiming.INITIAL),
                        new PropertyConfirmRequest.PropertyCostItem(
                                "선택 청소비", "선택 청소비", 30_000L, null,
                                ObligationStatus.OPTIONAL, false, CostTiming.INITIAL),
                        new PropertyConfirmRequest.PropertyCostItem(
                                "월 지원비", "월 지원비", 10_000L, null,
                                ObligationStatus.REQUIRED, true, CostTiming.MONTHLY)),
                rawResult);
    }

    private PropertyConfirmRequest confirmRequestWithUnknownInitialCost() {
        PropertyConfirmRequest request = confirmRequest();
        List<PropertyConfirmRequest.PropertyCostItem> costItems = new java.util.ArrayList<>(
                request.propertyCostItems());
        costItems.add(new PropertyConfirmRequest.PropertyCostItem(
                "화재 보험료", "화재 보험료", null, "금액 확인 필요",
                ObligationStatus.REQUIRED, true, CostTiming.INITIAL));

        return new PropertyConfirmRequest(
                request.sourceType(),
                request.modelVersion(),
                request.property(),
                List.copyOf(costItems),
                request.rawResult());
    }
}
