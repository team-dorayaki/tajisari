package com.tajisali.property.service;

import com.tajisali.common.constants.ExchangeRateConstants;
import com.tajisali.property.domain.Property;
import com.tajisali.property.domain.CostTiming;
import com.tajisali.property.domain.PropertyCostItem;
import com.tajisali.property.dto.PropertyDetailResponse;
import com.tajisali.property.dto.PropertyListResponse;
import com.tajisali.property.repository.PropertyRepository;
import com.tajisali.settlement.domain.CostCategory;
import com.tajisali.settlement.domain.CurrencyCode;
import com.tajisali.settlement.domain.SettlementPlan;
import com.tajisali.settlement.domain.SettlementPlanCostItem;
import com.tajisali.settlement.repository.SettlementPlanRepository;
import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.user.domain.User;
import com.tajisali.user.service.AnonymousUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class PropertyQueryService {

    private final PropertyRepository propertyRepository;
    private final SettlementPlanRepository settlementPlanRepository;
    private final AnonymousUserService anonymousUserService;
    private final PropertyCostCalculationService propertyCostCalculationService;

    @Transactional(readOnly = true)
    public PropertyListResponse getProperties(String userKey) {
        var user = anonymousUserService.findExisting(userKey);
        if (user.isEmpty()) {
            return new PropertyListResponse(0, java.util.List.of());
        }

        SettlementPlan settlementPlan = settlementPlanRepository
                .findByUserId(user.get().getId())
                .orElse(null);
        var properties = propertyRepository.findAllByUserIdOrderByCreatedAtDesc(user.get().getId()).stream()
                .map(property -> toSummary(property, settlementPlan))
                .toList();
        return new PropertyListResponse(properties.size(), properties);
    }

    @Transactional(readOnly = true)
    public PropertyDetailResponse getPropertyDetail(Long propertyId, String userKey) {
        User user = anonymousUserService.findExisting(userKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));
        Property property = propertyRepository.findByIdAndUserId(propertyId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));
        SettlementPlan plan = settlementPlanRepository
                .findByUserId(user.getId())
                .orElse(null);

        PropertyCostCalculationResult costCalculation = propertyCostCalculationService.calculate(property);

        return new PropertyDetailResponse(
                property.getId(),
                plan == null ? null : plan.getId(),
                new PropertyDetailResponse.PropertyInfo(
                        property.getPropertyName(),
                        property.getSourceSite(),
                        property.getSourceUrl(),
                        property.getPrefecture(),
                        property.getCity(),
                        property.getExclusiveAreaM2(),
                        property.getNearestStation(),
                        property.getWalkMinutes(),
                        property.getAvailableFrom(),
                        property.getContractPeriodMonths(),
                        property.getPriorityRank()),
                property.getImages().stream()
                        .map(image -> image.getStorageKey())
                        .toList(),
                new PropertyDetailResponse.CostAnalysis(
                        property.getRent(),
                        property.getManagementFee(),
                        property.getDeposit(),
                        property.getKeyMoney(),
                        costCalculation.initialCost(),
                        costCalculation.monthlyCost(),
                        costCalculation.refundableAmount(),
                        costCalculation.nonRefundableAmount(),
                        costCalculation.hasUnknownInitialCosts(),
                        costCalculation.hasUnknownMonthlyCosts(),
                        costCalculation.hasUnclassifiedCosts(),
                        property.getCostItems().stream().map(this::toCostItem).toList()),
                plan == null ? null : createSimulation(property, plan));
    }

    private PropertyDetailResponse.CostItem toCostItem(PropertyCostItem item) {
        return new PropertyDetailResponse.CostItem(
                item.getId(),
                item.getRawName(),
                item.getDisplayName(),
                item.getAmount(),
                item.getRawValue(),
                item.getObligationStatus(),
                item.isIncludedInCalculation(),
                item.isCalculated() && item.getAmount() != null
                        && (item.getTiming() == CostTiming.INITIAL
                        || item.getTiming() == CostTiming.MONTHLY),
                item.getTiming());
    }

    private PropertyDetailResponse.Simulation createSimulation(
            Property property, SettlementPlan plan) {
        long availableFunds = toJpy(plan.getPreparedFundsKrw() - plan.getEmergencyReserveKrw())
                .add(BigDecimal.valueOf(plan.getPreparedFundsJpy() - plan.getEmergencyReserveJpy()))
                .setScale(0, RoundingMode.DOWN)
                .longValue();
        long initialCost = valueOrZero(initialCostOf(property))
                + costItemTotalInJpy(plan, CostCategory.INITIAL)
                .setScale(0, RoundingMode.DOWN)
                .longValue();
        long monthlyHousingCost = monthlyCostOf(property);
        long monthlyLivingCost = costItemTotalInJpy(plan, CostCategory.MONTHLY)
                .setScale(0, RoundingMode.DOWN)
                .longValue();
        long totalMonthlyCost = monthlyHousingCost + monthlyLivingCost;
        long balanceAfterMoveIn = availableFunds - initialCost;
        BigDecimal livingMonths = totalMonthlyCost == 0
                ? null
                : BigDecimal.valueOf(Math.max(balanceAfterMoveIn, 0L))
                .divide(BigDecimal.valueOf(totalMonthlyCost), 1, RoundingMode.DOWN);
        long balanceAfterPlannedStay = balanceAfterMoveIn
                - totalMonthlyCost * plan.getPlannedStayMonths();

        return new PropertyDetailResponse.Simulation(
                new PropertyDetailResponse.ExchangeRate(
                        100, ExchangeRateConstants.KRW_PER_100_JPY.intValueExact()),
                availableFunds,
                initialCost,
                balanceAfterMoveIn,
                monthlyHousingCost,
                monthlyLivingCost,
                totalMonthlyCost,
                livingMonths,
                plan.getPlannedStayMonths(),
                balanceAfterPlannedStay,
                Math.max(-balanceAfterPlannedStay, 0L));
    }

    private PropertyListResponse.PropertySummary toSummary(
            Property property, SettlementPlan settlementPlan) {
        String thumbnailUrl = property.getImages().isEmpty()
                ? null
                : property.getImages().getFirst().getStorageKey();

        return new PropertyListResponse.PropertySummary(
                property.getId(),
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
