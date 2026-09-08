package com.tajisali.property.service;

import com.tajisali.common.constants.ExchangeRateConstants;
import com.tajisali.property.domain.Property;
import com.tajisali.property.dto.PropertyListResponse;
import com.tajisali.property.repository.PropertyRepository;
import com.tajisali.property.repository.PropertyAiAnalysisRepository;
import com.tajisali.settlement.domain.CostCategory;
import com.tajisali.settlement.domain.CurrencyCode;
import com.tajisali.settlement.domain.SettlementPlan;
import com.tajisali.settlement.domain.SettlementPlanCostItem;
import com.tajisali.settlement.repository.SettlementPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class PropertyQueryService {

    private final PropertyRepository propertyRepository;
    private final PropertyAiAnalysisRepository propertyAiAnalysisRepository;
    private final SettlementPlanRepository settlementPlanRepository;

    @Transactional(readOnly = true)
    public PropertyListResponse getProperties() {
        SettlementPlan settlementPlan = settlementPlanRepository
                .findTopByOrderByCreatedAtDesc()
                .orElse(null);
        var properties = propertyRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(property -> toSummary(property, settlementPlan))
                .toList();
        return new PropertyListResponse(properties.size(), properties);
    }

    private PropertyListResponse.PropertySummary toSummary(
            Property property, SettlementPlan settlementPlan) {
        Long analysisId = propertyAiAnalysisRepository
                .findTopByPropertyIdOrderByIdDesc(property.getId())
                .map(analysis -> analysis.getId())
                .orElse(null);
        String thumbnailUrl = property.getImages().isEmpty()
                ? null
                : property.getImages().getFirst().getStorageKey();

        return new PropertyListResponse.PropertySummary(
                property.getId(),
                analysisId,
                property.getPropertyName(),
                property.getRent(),
                initialCostOf(property),
                calculateLivingMonths(property, settlementPlan),
                property.getPriorityRank(),
                thumbnailUrl);
    }

    private Long initialCostOf(Property property) {
        return property.getConfirmedInitialCost() != null
                ? property.getConfirmedInitialCost()
                : property.getListedInitialCostTotal();
    }

    private BigDecimal calculateLivingMonths(Property property, SettlementPlan plan) {
        if (plan == null) {
            return null;
        }

        BigDecimal usableFunds = toJpy(plan.getPreparedFundsKrw() - plan.getEmergencyReserveKrw())
                .add(BigDecimal.valueOf(plan.getPreparedFundsJpy() - plan.getEmergencyReserveJpy()));
        BigDecimal initialCosts = BigDecimal.valueOf(valueOrZero(initialCostOf(property)))
                .add(costItemTotalInJpy(plan, CostCategory.INITIAL));
        BigDecimal monthlyCosts = BigDecimal.valueOf(monthlyCostOf(property))
                .add(costItemTotalInJpy(plan, CostCategory.MONTHLY));

        if (monthlyCosts.signum() == 0) {
            return null;
        }

        BigDecimal remainingFunds = usableFunds.subtract(initialCosts).max(BigDecimal.ZERO);
        return remainingFunds.divide(monthlyCosts, 1, RoundingMode.DOWN);
    }

    private BigDecimal costItemTotalInJpy(SettlementPlan plan, CostCategory category) {
        BigDecimal total = BigDecimal.ZERO;
        for (SettlementPlanCostItem item : plan.getCostItems()) {
            if (item.getCostCategory() != category) {
                continue;
            }
            BigDecimal amount = BigDecimal.valueOf(item.getAmount());
            total = total.add(item.getCurrency() == CurrencyCode.KRW ? toJpy(amount) : amount);
        }
        return total;
    }

    private BigDecimal toJpy(long krw) {
        return toJpy(BigDecimal.valueOf(krw));
    }

    private BigDecimal toJpy(BigDecimal krw) {
        return krw.multiply(BigDecimal.valueOf(100))
                .divide(ExchangeRateConstants.KRW_PER_100_JPY, 10, RoundingMode.HALF_UP);
    }

    private long monthlyCostOf(Property property) {
        if (property.getConfirmedMonthlyCost() != null) {
            return property.getConfirmedMonthlyCost();
        }
        return valueOrZero(property.getRent()) + valueOrZero(property.getManagementFee());
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

}
