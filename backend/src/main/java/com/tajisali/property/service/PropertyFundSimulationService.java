package com.tajisali.property.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.exchange.service.ExchangeRateService;
import com.tajisali.settlement.domain.CostCategory;
import com.tajisali.settlement.domain.CurrencyCode;
import com.tajisali.settlement.domain.SettlementPlan;
import com.tajisali.settlement.domain.SettlementPlanCostItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyFundSimulationService {

    private final ExchangeRateService exchangeRateService;

    public PropertyFundSimulationResult calculate(
            long confirmedInitialCost,
            long confirmedMonthlyCost,
            SettlementPlan settlementPlan) {
        long availableFunds = calculateAvailableFunds(settlementPlan);
        long initialCost = add(
                confirmedInitialCost,
                costItemTotalInJpy(settlementPlan, CostCategory.INITIAL));
        long monthlyLivingCost = costItemTotalInJpy(settlementPlan, CostCategory.MONTHLY);
        long totalMonthlyCost = add(confirmedMonthlyCost, monthlyLivingCost);
        long balanceAfterMoveIn = subtract(availableFunds, initialCost);
        boolean canMoveIn = balanceAfterMoveIn >= 0;
        boolean isUnlimited = canMoveIn && totalMonthlyCost == 0;

        long requiredFunds = add(
                initialCost,
                multiply(totalMonthlyCost, settlementPlan.getPlannedStayMonths()));
        long surplus = Math.max(subtract(availableFunds, requiredFunds), 0L);
        long shortageJpy = Math.max(subtract(requiredFunds, availableFunds), 0L);

        return new PropertyFundSimulationResult(
                availableFunds,
                initialCost,
                canMoveIn,
                balanceAfterMoveIn,
                confirmedMonthlyCost,
                monthlyLivingCost,
                totalMonthlyCost,
                createMonthlyBalances(settlementPlan, balanceAfterMoveIn, totalMonthlyCost, canMoveIn),
                calculateLivingMonths(balanceAfterMoveIn, totalMonthlyCost, canMoveIn),
                isUnlimited,
                settlementPlan.getPlannedStayMonths(),
                requiredFunds,
                surplus,
                shortageJpy,
                exchangeRateService.convertJpyToKrw(shortageJpy));
    }

    private long calculateAvailableFunds(SettlementPlan settlementPlan) {
        long availableKrw = subtract(
                settlementPlan.getPreparedFundsKrw(),
                settlementPlan.getEmergencyReserveKrw());
        long availableJpy = subtract(
                settlementPlan.getPreparedFundsJpy(),
                settlementPlan.getEmergencyReserveJpy());
        return add(exchangeRateService.convertKrwToJpy(availableKrw), availableJpy);
    }

    private long costItemTotalInJpy(SettlementPlan settlementPlan, CostCategory category) {
        long total = 0L;
        for (SettlementPlanCostItem item : settlementPlan.getCostItems()) {
            if (item.getCostCategory() != category) {
                continue;
            }
            long amount = item.getCurrency() == CurrencyCode.KRW
                    ? exchangeRateService.convertKrwToJpy(item.getAmount())
                    : item.getAmount();
            total = add(total, amount);
        }
        return total;
    }

    private List<PropertyFundSimulationResult.MonthlyBalance> createMonthlyBalances(
            SettlementPlan settlementPlan,
            long balanceAfterMoveIn,
            long totalMonthlyCost,
            boolean canMoveIn) {
        if (!canMoveIn) {
            return List.of();
        }

        List<PropertyFundSimulationResult.MonthlyBalance> monthlyBalances = new ArrayList<>();
        long balance = balanceAfterMoveIn;
        for (int month = 1; month <= settlementPlan.getPlannedStayMonths(); month++) {
            balance = subtract(balance, totalMonthlyCost);
            monthlyBalances.add(new PropertyFundSimulationResult.MonthlyBalance(month, balance));
        }
        return List.copyOf(monthlyBalances);
    }

    private BigDecimal calculateLivingMonths(
            long balanceAfterMoveIn,
            long totalMonthlyCost,
            boolean canMoveIn) {
        if (!canMoveIn || totalMonthlyCost == 0) {
            return null;
        }
        return BigDecimal.valueOf(balanceAfterMoveIn)
                .divide(BigDecimal.valueOf(totalMonthlyCost), 1, RoundingMode.DOWN);
    }

    private long add(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw new BusinessException(ErrorCode.PROPERTY_FUND_SIMULATION_TOTAL_OVERFLOW, exception);
        }
    }

    private long subtract(long left, long right) {
        try {
            return Math.subtractExact(left, right);
        } catch (ArithmeticException exception) {
            throw new BusinessException(ErrorCode.PROPERTY_FUND_SIMULATION_TOTAL_OVERFLOW, exception);
        }
    }

    private long multiply(long left, int right) {
        try {
            return Math.multiplyExact(left, right);
        } catch (ArithmeticException exception) {
            throw new BusinessException(ErrorCode.PROPERTY_FUND_SIMULATION_TOTAL_OVERFLOW, exception);
        }
    }
}
