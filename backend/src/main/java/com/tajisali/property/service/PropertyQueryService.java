package com.tajisali.property.service;

import com.tajisali.common.constants.ExchangeRateConstants;
import com.tajisali.property.domain.Property;
import com.tajisali.property.domain.CostTiming;
import com.tajisali.property.domain.ObligationStatus;
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
import java.util.ArrayList;
import java.util.List;

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
        PropertyDetailCostPresentation costPresentation = createCostPresentation(
                property, costCalculation);

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
                        property.getPriorityRank(),
                        property.getRent(),
                        property.getManagementFee()),
                property.getImages().stream()
                        .map(image -> new PropertyDetailResponse.PropertyImage(
                                image.getId(),
                                image.getStorageKey(),
                                image.getImageOrder()))
                        .toList(),
                new PropertyDetailResponse.CostAnalysis(
                        costPresentation.summary(),
                        costPresentation.costGroups(),
                        costPresentation.excludedCosts()),
                plan == null ? null : createSimulation(plan, costCalculation));
    }

    private PropertyDetailResponse.CostItem toCostItem(PropertyCostItem item) {
        boolean includedInTotal = isIncludedInCurrentTotal(item);
        return new PropertyDetailResponse.CostItem(
                String.valueOf(item.getId()),
                item.getDisplayName(),
                item.getAmount(),
                item.getRawValue(),
                item.getObligationStatus() == ObligationStatus.OPTIONAL,
                item.getObligationStatus() != ObligationStatus.OPTIONAL
                        || item.isIncludedInCalculation(),
                includedInTotal,
                item.getTiming() == CostTiming.CONDITIONAL,
                item.getTiming());
    }

    private PropertyDetailCostPresentation createCostPresentation(
            Property property,
            PropertyCostCalculationResult calculation) {
        List<PropertyDetailResponse.CostItem> monthlyItems = new ArrayList<>();
        List<PropertyDetailResponse.CostItem> moveInItems = new ArrayList<>();
        List<PropertyDetailResponse.CostItem> optionalItems = new ArrayList<>();
        List<PropertyDetailResponse.CostItem> futureItems = new ArrayList<>();
        List<PropertyDetailResponse.ExcludedCost> excludedCosts = new ArrayList<>();

        addBaseCost(monthlyItems, "rent", "월세(家賃)", property.getRent(), CostTiming.MONTHLY);
        addBaseCost(monthlyItems, "management", "관리비·공익비",
                property.getManagementFee(), CostTiming.MONTHLY);
        addBaseCost(moveInItems, "deposit", "시키킨(敷金)",
                property.getDeposit(), CostTiming.INITIAL);
        addBaseCost(moveInItems, "key-money", "레이킨(礼金)",
                property.getKeyMoney(), CostTiming.INITIAL);

        for (PropertyCostItem item : property.getCostItems()) {
            if (isExcluded(item)) {
                excludedCosts.add(new PropertyDetailResponse.ExcludedCost(
                        item.getDisplayName(), categoryLabel(item.getTiming()), exclusionReason(item)));
                continue;
            }

            PropertyDetailResponse.CostItem displayItem = toCostItem(item);
            if (item.getTiming() == CostTiming.MONTHLY) {
                if (item.getObligationStatus() != ObligationStatus.OPTIONAL
                        || item.isIncludedInCalculation()) {
                    monthlyItems.add(displayItem);
                }
            } else if (item.getTiming() == CostTiming.INITIAL
                    && item.getObligationStatus() == ObligationStatus.OPTIONAL) {
                if (item.isIncludedInCalculation()) {
                    optionalItems.add(displayItem);
                }
            } else if (item.getTiming() == CostTiming.INITIAL) {
                moveInItems.add(displayItem);
            } else {
                futureItems.add(displayItem);
            }
        }

        long contractMoveInCost = sumIncluded(moveInItems);
        long selectedOptionalCost = sumIncluded(optionalItems);
        long minimumInitialCost = contractMoveInCost;
        long estimatedMoveOutCost = futureItems.stream()
                .filter(item -> item.timing() == CostTiming.MOVE_OUT)
                .filter(item -> item.amount() != null)
                .filter(item -> !item.optional() || item.selected())
                .mapToLong(PropertyDetailResponse.CostItem::amount)
                .sum();

        PropertyDetailResponse.CostSummary summary = new PropertyDetailResponse.CostSummary(
                calculation.initialCost(),
                minimumInitialCost,
                calculation.monthlyCost(),
                contractMoveInCost,
                selectedOptionalCost,
                calculation.refundableAmount(),
                calculation.nonRefundableAmount(),
                estimatedMoveOutCost);

        return new PropertyDetailCostPresentation(
                summary,
                new PropertyDetailResponse.CostGroups(
                        List.copyOf(monthlyItems),
                        List.copyOf(moveInItems),
                        List.copyOf(optionalItems),
                        List.copyOf(futureItems)),
                List.copyOf(excludedCosts));
    }

    private void addBaseCost(
            List<PropertyDetailResponse.CostItem> target,
            String id,
            String label,
            Long amount,
            CostTiming timing) {
        target.add(new PropertyDetailResponse.CostItem(
                id, label, amount, null, false, true, amount != null,
                false, timing));
    }

    private long sumIncluded(List<PropertyDetailResponse.CostItem> items) {
        return items.stream()
                .filter(PropertyDetailResponse.CostItem::includedInTotal)
                .filter(item -> item.amount() != null)
                .mapToLong(PropertyDetailResponse.CostItem::amount)
                .sum();
    }

    private boolean isIncludedInCurrentTotal(PropertyCostItem item) {
        return item.getAmount() != null
                && (item.getTiming() == CostTiming.INITIAL
                || item.getTiming() == CostTiming.MONTHLY)
                && item.isCalculated();
    }

    private boolean isExcluded(PropertyCostItem item) {
        return item.getAmount() == null
                || item.getObligationStatus() == ObligationStatus.UNKNOWN
                || item.getTiming() == CostTiming.UNKNOWN;
    }

    private String exclusionReason(PropertyCostItem item) {
        if (item.getAmount() == null) {
            return "금액 미확인";
        }
        if (item.getObligationStatus() == ObligationStatus.UNKNOWN) {
            return "필수 여부 미확인";
        }
        return "발생 시점 미확인";
    }

    private String categoryLabel(CostTiming timing) {
        return switch (timing) {
            case INITIAL -> "계약·입주 시";
            case MONTHLY -> "매월 반복비용";
            case RENEWAL, MOVE_OUT, CONDITIONAL -> "계약 후";
            case UNKNOWN -> "분류 미확인";
        };
    }

    private PropertyDetailResponse.Simulation createSimulation(
            SettlementPlan plan,
            PropertyCostCalculationResult propertyCost) {
        long availableFunds = toJpy(plan.getPreparedFundsKrw() - plan.getEmergencyReserveKrw())
                .add(BigDecimal.valueOf(plan.getPreparedFundsJpy() - plan.getEmergencyReserveJpy()))
                .setScale(0, RoundingMode.DOWN)
                .longValue();
        long initialCost = valueOrZero(propertyCost.initialCost())
                + costItemTotalInJpy(plan, CostCategory.INITIAL)
                .setScale(0, RoundingMode.DOWN)
                .longValue();
        long monthlyHousingCost = valueOrZero(propertyCost.monthlyCost());
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
        boolean canCoverPlannedStay = balanceAfterPlannedStay >= 0;
        long additionalFundsRequired = Math.max(-balanceAfterPlannedStay, 0L);
        long additionalFundsRequiredKrw = BigDecimal.valueOf(additionalFundsRequired)
                .multiply(ExchangeRateConstants.KRW_PER_100_JPY)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.UP)
                .longValue();
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
                canCoverPlannedStay,
                additionalFundsRequired,
                additionalFundsRequiredKrw);
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

    private record PropertyDetailCostPresentation(
            PropertyDetailResponse.CostSummary summary,
            PropertyDetailResponse.CostGroups costGroups,
            List<PropertyDetailResponse.ExcludedCost> excludedCosts) {
    }

}
