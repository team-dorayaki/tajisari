package com.tajisali.property.service;

import java.math.BigDecimal;
import java.util.List;

public record PropertyFundSimulationResult(
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

    public record MonthlyBalance(int month, long balance) {
    }
}
