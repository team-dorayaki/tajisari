package com.tajisali.property.service;

public record PropertyCostCalculationResult(
        Long initialCost,
        Long monthlyCost,
        Long refundableAmount,
        Long nonRefundableAmount,
        boolean hasUnknownInitialCosts,
        boolean hasUnknownMonthlyCosts,
        boolean hasUnclassifiedCosts) {
}
