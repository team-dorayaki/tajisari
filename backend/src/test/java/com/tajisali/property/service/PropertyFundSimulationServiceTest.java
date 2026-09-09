package com.tajisali.property.service;

import com.tajisali.exchange.service.ExchangeRateService;
import com.tajisali.settlement.domain.CostCategory;
import com.tajisali.settlement.domain.CostType;
import com.tajisali.settlement.domain.CurrencyCode;
import com.tajisali.settlement.domain.MonthlyLivingCostInputMethod;
import com.tajisali.settlement.domain.SettlementPlan;
import com.tajisali.settlement.domain.SettlementPlanCostItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyFundSimulationServiceTest {

    private final PropertyFundSimulationService simulationService =
            new PropertyFundSimulationService(new ExchangeRateService());

    @Test
    void 확정_매물비용과_DIRECT_생활비를_기준으로_자금을_시뮬레이션한다() {
        SettlementPlan plan = plan(3, 8_600_000L, 100_000L, 0L, 0L);
        plan.addCostItem(costItem(CostCategory.INITIAL, CostType.AIRFARE, 20_000L, CurrencyCode.JPY));
        plan.addCostItem(costItem(CostCategory.MONTHLY, CostType.FOOD, 100_000L, CurrencyCode.JPY));

        PropertyFundSimulationResult result = simulationService.calculate(300_000L, 80_000L, plan);

        assertThat(result.availableFunds()).isEqualTo(1_100_000L);
        assertThat(result.initialCost()).isEqualTo(320_000L);
        assertThat(result.canMoveIn()).isTrue();
        assertThat(result.balanceAfterMoveIn()).isEqualTo(780_000L);
        assertThat(result.monthlyHousingCost()).isEqualTo(80_000L);
        assertThat(result.monthlyLivingCost()).isEqualTo(100_000L);
        assertThat(result.totalMonthlyCost()).isEqualTo(180_000L);
        assertThat(result.monthlyBalances())
                .extracting(
                        PropertyFundSimulationResult.MonthlyBalance::month,
                        PropertyFundSimulationResult.MonthlyBalance::balance)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1, 600_000L),
                        org.assertj.core.groups.Tuple.tuple(2, 420_000L),
                        org.assertj.core.groups.Tuple.tuple(3, 240_000L));
        assertThat(result.livingMonths()).isEqualByComparingTo("4.3");
        assertThat(result.isUnlimited()).isFalse();
        assertThat(result.requiredFunds()).isEqualTo(860_000L);
        assertThat(result.surplus()).isEqualTo(240_000L);
        assertThat(result.shortageJpy()).isZero();
        assertThat(result.shortageKrw()).isZero();
    }

    @Test
    void 원화_비상예비비를_뺀_금액을_엔화로_환산한다() {
        SettlementPlan plan = plan(1, 1_000L, 10L, 140L, 0L);

        PropertyFundSimulationResult result = simulationService.calculate(0L, 0L, plan);

        assertThat(result.availableFunds()).isEqualTo(110L);
    }

    @Test
    void 초기비용을_낼_수_없으면_입주불가로_처리한다() {
        SettlementPlan plan = plan(3, 860_000L, 0L, 0L, 0L);

        PropertyFundSimulationResult result = simulationService.calculate(150_000L, 80_000L, plan);

        assertThat(result.canMoveIn()).isFalse();
        assertThat(result.balanceAfterMoveIn()).isEqualTo(-50_000L);
        assertThat(result.monthlyBalances()).isEmpty();
        assertThat(result.livingMonths()).isNull();
        assertThat(result.isUnlimited()).isFalse();
    }

    @Test
    void 월_지출이_모두_0원이면_무기한으로_처리한다() {
        SettlementPlan plan = plan(2, 860_000L, 0L, 0L, 0L);

        PropertyFundSimulationResult result = simulationService.calculate(50_000L, 0L, plan);

        assertThat(result.monthlyLivingCost()).isZero();
        assertThat(result.isUnlimited()).isTrue();
        assertThat(result.livingMonths()).isNull();
        assertThat(result.monthlyBalances())
                .extracting(PropertyFundSimulationResult.MonthlyBalance::balance)
                .containsExactly(50_000L, 50_000L);
    }

    @Test
    void 부족금액을_엔화와_원화로_반환한다() {
        SettlementPlan plan = plan(2, 860_000L, 0L, 0L, 0L);
        plan.addCostItem(costItem(CostCategory.MONTHLY, CostType.FOOD, 50_000L, CurrencyCode.JPY));

        PropertyFundSimulationResult result = simulationService.calculate(150_000L, 100_000L, plan);

        assertThat(result.requiredFunds()).isEqualTo(450_000L);
        assertThat(result.surplus()).isZero();
        assertThat(result.shortageJpy()).isEqualTo(350_000L);
        assertThat(result.shortageKrw()).isEqualTo(3_010_000L);
    }

    @Test
    void 원화_생활비도_기준_환율로_환산한다() {
        SettlementPlan plan = plan(1, 860_000L, 0L, 0L, 0L);
        plan.addCostItem(costItem(CostCategory.MONTHLY, CostType.FOOD, 8_600L, CurrencyCode.KRW));

        PropertyFundSimulationResult result = simulationService.calculate(0L, 0L, plan);

        assertThat(result.monthlyLivingCost()).isEqualTo(1_000L);
    }

    private SettlementPlan plan(
            int plannedStayMonths,
            long preparedFundsKrw,
            long preparedFundsJpy,
            long emergencyReserveKrw,
            long emergencyReserveJpy) {
        return new SettlementPlan(
                LocalDate.now().plusMonths(1),
                plannedStayMonths,
                preparedFundsKrw,
                preparedFundsJpy,
                emergencyReserveKrw,
                emergencyReserveJpy,
                MonthlyLivingCostInputMethod.DIRECT);
    }

    private SettlementPlanCostItem costItem(
            CostCategory category,
            CostType type,
            long amount,
            CurrencyCode currency) {
        return new SettlementPlanCostItem(category, type, amount, currency);
    }
}
