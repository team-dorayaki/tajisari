package com.tajisali.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SettlementPlanCreateResponse {

    private final Long planId;
    private final CurrencyTotalsResponse additionalInitialCostTotals;
    private final CurrencyTotalsResponse monthlyLivingCostTotals;
    private final String status;
}
