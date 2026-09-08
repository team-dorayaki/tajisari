package com.tajisali.settlement.dto;

import com.tajisali.settlement.domain.CostType;
import com.tajisali.settlement.domain.CurrencyCode;
import com.tajisali.settlement.domain.MonthlyLivingCostInputMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class SettlementPlanResponse {

    private final Long planId;
    private final LocalDate moveInDate;
    private final int plannedStayMonths;
    private final CurrencyTotalsResponse preparedFunds;
    private final CurrencyTotalsResponse emergencyReserve;
    private final List<CostItem> additionalInitialCosts;
    private final List<CostItem> monthlyLivingCosts;
    private final MonthlyLivingCostInputMethod monthlyLivingCostInputMethod;
    private final CurrencyTotalsResponse additionalInitialCostTotals;
    private final CurrencyTotalsResponse monthlyLivingCostTotals;

    public record CostItem(CostType type, long amount, CurrencyCode currency) {
    }
}
