package com.tajisali.property.dto;

import com.tajisali.property.domain.CostTiming;
import com.tajisali.property.domain.ObligationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PropertyDetailResponse(
        Long propertyId,
        Long settlementPlanId,
        PropertyInfo property,
        List<String> imageUrls,
        CostAnalysis costAnalysis,
        Simulation simulation) {

    public record PropertyInfo(
            String name,
            String sourceSite,
            String sourceUrl,
            String prefecture,
            String city,
            BigDecimal area,
            String nearestStation,
            Integer walkMinutes,
            LocalDate availableFrom,
            Integer contractPeriodMonths,
            Integer priorityRank) {
    }

    public record CostAnalysis(
            Long rent,
            Long managementFee,
            Long deposit,
            Long keyMoney,
            Long initialCost,
            Long monthlyCost,
            Long refundableAmount,
            Long nonRefundableAmount,
            boolean hasUnknownInitialCosts,
            boolean hasUnknownMonthlyCosts,
            boolean hasUnclassifiedCosts,
            List<CostItem> costItems) {
    }

    public record CostItem(
            Long costItemId,
            String rawName,
            String displayName,
            Long amount,
            String rawValue,
            ObligationStatus obligationStatus,
            boolean includedInCalculation,
            boolean calculated,
            CostTiming timing) {
    }

    public record Simulation(
            ExchangeRate exchangeRate,
            long availableFunds,
            long initialCost,
            boolean canMoveIn,
            long balanceAfterMoveIn,
            long monthlyHousingCost,
            long monthlyLivingCost,
            long totalMonthlyCost,
            List<MonthlyBalance> monthlyBalances,
            BigDecimal livingMonths,
            boolean isUnlimited,
            int plannedStayMonths,
            long requiredFunds,
            long surplus,
            long shortageJpy,
            long shortageKrw) {
    }

    public record MonthlyBalance(int month, long balance) {
    }

    public record ExchangeRate(int jpy, int krw) {
    }
}
