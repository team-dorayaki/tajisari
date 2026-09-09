package com.tajisali.property.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.property.domain.CostTiming;
import com.tajisali.property.domain.ObligationStatus;
import com.tajisali.property.domain.Property;
import com.tajisali.property.domain.PropertyCostItem;
import org.springframework.stereotype.Service;

@Service
public class PropertyCostCalculationService {

    public PropertyCostCalculationResult calculate(Property property) {
        AmountSummary initialCost = new AmountSummary();
        AmountSummary monthlyCost = new AmountSummary();
        AmountSummary nonRefundableAmount = new AmountSummary();
        boolean hasUnclassifiedCosts = false;

        addBaseAmount(initialCost, property.getDeposit());
        addBaseAmount(initialCost, property.getKeyMoney());
        addBaseAmount(nonRefundableAmount, property.getKeyMoney());

        addBaseAmount(monthlyCost, property.getRent());
        addBaseAmount(monthlyCost, property.getManagementFee());

        for (PropertyCostItem item : property.getCostItems()) {
            if (item.getTiming() == CostTiming.INITIAL) {
                addInitialCostItem(initialCost, nonRefundableAmount, item);
            } else if (item.getTiming() == CostTiming.MONTHLY) {
                addMonthlyCostItem(monthlyCost, item);
            } else if (item.getTiming() == CostTiming.UNKNOWN && requiresReview(item)) {
                hasUnclassifiedCosts = true;
            }
        }

        return new PropertyCostCalculationResult(
                initialCost.total(),
                monthlyCost.total(),
                property.getDeposit(),
                nonRefundableAmount.total(),
                initialCost.hasUnknownAmount(),
                monthlyCost.hasUnknownAmount(),
                hasUnclassifiedCosts);
    }

    private void addInitialCostItem(
            AmountSummary initialCost,
            AmountSummary nonRefundableAmount,
            PropertyCostItem item) {
        if (item.getObligationStatus() == ObligationStatus.UNKNOWN) {
            initialCost.markUnknown();
            nonRefundableAmount.markUnknown();
            return;
        }
        if (!isIncluded(item)) {
            return;
        }
        if (item.getAmount() == null) {
            initialCost.markUnknown();
            nonRefundableAmount.markUnknown();
            return;
        }
        initialCost.add(item.getAmount());
        nonRefundableAmount.add(item.getAmount());
    }

    private void addMonthlyCostItem(AmountSummary monthlyCost, PropertyCostItem item) {
        if (item.getObligationStatus() == ObligationStatus.UNKNOWN) {
            monthlyCost.markUnknown();
            return;
        }
        if (!isIncluded(item)) {
            return;
        }
        if (item.getAmount() == null) {
            monthlyCost.markUnknown();
            return;
        }
        monthlyCost.add(item.getAmount());
    }

    private void addBaseAmount(AmountSummary summary, Long amount) {
        if (amount == null) {
            summary.markUnknown();
            return;
        }
        summary.add(amount);
    }

    private boolean isIncluded(PropertyCostItem item) {
        return item.getObligationStatus() == ObligationStatus.REQUIRED
                || (item.getObligationStatus() == ObligationStatus.OPTIONAL
                && item.isIncludedInCalculation());
    }

    private boolean requiresReview(PropertyCostItem item) {
        return item.getObligationStatus() != ObligationStatus.OPTIONAL
                || item.isIncludedInCalculation();
    }

    private static class AmountSummary {

        private Long total;
        private boolean hasUnknownAmount;

        void add(long amount) {
            try {
                total = total == null ? amount : Math.addExact(total, amount);
            } catch (ArithmeticException exception) {
                throw new BusinessException(ErrorCode.PROPERTY_COST_TOTAL_OVERFLOW, exception);
            }
        }

        void markUnknown() {
            hasUnknownAmount = true;
        }

        Long total() {
            return total;
        }

        boolean hasUnknownAmount() {
            return hasUnknownAmount;
        }
    }
}
