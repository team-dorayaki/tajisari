package com.tajisali.property.dto;

import java.math.BigDecimal;
import java.util.List;

public record PropertyComparisonResponse(
        Long settlementPlanId,
        ExchangeRate exchangeRate,
        List<PropertyComparison> properties) {

    public record ExchangeRate(int jpy, int krw) {
    }

    public record PropertyComparison(
            Long propertyId,
            String propertyName,
            String thumbnailUrl,
            Integer priorityRank,
            Conditions conditions,
            Costs costs,
            Simulation simulation,
            Highlights highlights) {
    }

    public record Conditions(
            String prefecture,
            String city,
            String nearestStation,
            Integer walkMinutes,
            BigDecimal exclusiveAreaM2,
            Integer contractPeriodMonths) {
    }

    public record Costs(
            Long confirmedInitialCost,
            Long confirmedMonthlyCost,
            Long initialSettlementCost,
            Long refundableAmount,
            Long nonRefundableAmount,
            Long plannedStayHousingCost,
            int unknownCostItemCount,
            boolean hasUnknownCosts) {
    }

    public record Simulation(
            boolean canMoveIn,
            Long balanceAfterMoveIn,
            Long monthlyHousingCost,
            Long monthlyLivingCost,
            Long totalMonthlyCost,
            List<MonthlyBalance> monthlyBalances,
            BigDecimal livingMonths,
            boolean unlimited,
            Long requiredFunds,
            Long surplus,
            Long shortageJpy,
            Long shortageKrw) {
    }

    public record MonthlyBalance(int month, long balance) {
    }

    public record Highlights(
            boolean lowestInitialSettlementCost,
            boolean lowestMonthlyHousingCost,
            boolean longestLivingMonths) {
    }
}
