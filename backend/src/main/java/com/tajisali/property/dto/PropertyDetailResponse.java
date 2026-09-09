package com.tajisali.property.dto;

import com.tajisali.property.domain.CostTiming;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PropertyDetailResponse(
        Long propertyId,
        Long settlementPlanId,
        PropertyInfo property,
        List<PropertyImage> images,
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
            Integer priorityRank,
            Long rent,
            Long managementFee) {
    }

    public record PropertyImage(Long imageId, String imageUrl, int order) {
    }

    public record CostAnalysis(
            CostSummary summary,
            CostGroups costGroups,
            List<ExcludedCost> excludedCosts) {
    }

    public record CostSummary(
            Long initialCost,
            long minimumInitialCost,
            Long monthlyCost,
            long contractMoveInCost,
            long selectedOptionalCost,
            Long refundableAmount,
            Long nonRefundableAmount,
            long estimatedMoveOutCost) {
    }

    public record CostGroups(
            List<CostItem> monthly,
            List<CostItem> moveIn,
            List<CostItem> optional,
            List<CostItem> future) {
    }

    public record CostItem(
            String id,
            String label,
            Long amount,
            String originalText,
            boolean optional,
            boolean selected,
            boolean includedInTotal,
            boolean conditional,
            CostTiming timing) {
    }

    public record ExcludedCost(String label, String category, String reason) {
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
