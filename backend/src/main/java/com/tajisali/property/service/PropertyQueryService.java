package com.tajisali.property.service;

import com.tajisali.exchange.service.ExchangeRateService;
import com.tajisali.property.domain.Property;
import com.tajisali.property.domain.CostTiming;
import com.tajisali.property.domain.ObligationStatus;
import com.tajisali.property.domain.PropertyCostItem;
import com.tajisali.property.dto.PropertyDetailResponse;
import com.tajisali.property.dto.PropertyListResponse;
import com.tajisali.property.repository.PropertyRepository;
import com.tajisali.settlement.domain.MonthlyLivingCostInputMethod;
import com.tajisali.settlement.domain.SettlementPlan;
import com.tajisali.settlement.repository.SettlementPlanRepository;
import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.user.domain.User;
import com.tajisali.user.service.AnonymousUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyQueryService {

    private final PropertyRepository propertyRepository;
    private final SettlementPlanRepository settlementPlanRepository;
    private final AnonymousUserService anonymousUserService;
    private final PropertyCostCalculationService propertyCostCalculationService;
    private final PropertyFundSimulationService propertyFundSimulationService;

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
                                image.getId(), image.getStorageKey(), image.getImageOrder()))
                        .toList(),
                new PropertyDetailResponse.CostAnalysis(
                        costPresentation.summary(),
                        costPresentation.costGroups(),
                        costPresentation.excludedCosts()),
                createSimulation(property, plan));
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
        long estimatedMoveOutCost = futureItems.stream()
                .filter(item -> item.timing() == CostTiming.MOVE_OUT)
                .filter(item -> item.amount() != null)
                .filter(item -> !item.optional() || item.selected())
                .mapToLong(PropertyDetailResponse.CostItem::amount)
                .sum();

        PropertyDetailResponse.CostSummary summary = new PropertyDetailResponse.CostSummary(
                calculation.initialCost(),
                contractMoveInCost,
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
            Property property, SettlementPlan plan) {
        if (plan == null
                || plan.getMonthlyLivingCostInputMethod() != MonthlyLivingCostInputMethod.DIRECT
                || property.getConfirmedInitialCost() == null
                || property.getConfirmedMonthlyCost() == null) {
            return null;
        }

        PropertyFundSimulationResult result = propertyFundSimulationService.calculate(
                property.getConfirmedInitialCost(),
                property.getConfirmedMonthlyCost(),
                plan);
        return new PropertyDetailResponse.Simulation(
                new PropertyDetailResponse.ExchangeRate(
                        Math.toIntExact(ExchangeRateService.BASE_JPY),
                        Math.toIntExact(ExchangeRateService.KRW_PER_100_JPY)),
                result.availableFunds(),
                result.initialCost(),
                result.canMoveIn(),
                result.balanceAfterMoveIn(),
                result.monthlyHousingCost(),
                result.monthlyLivingCost(),
                result.totalMonthlyCost(),
                result.monthlyBalances().stream()
                        .map(balance -> new PropertyDetailResponse.MonthlyBalance(
                                balance.month(), balance.balance()))
                        .toList(),
                result.livingMonths(),
                result.isUnlimited(),
                result.plannedStayMonths(),
                result.requiredFunds(),
                result.surplus(),
                result.shortageJpy(),
                result.shortageKrw());
    }

    private PropertyListResponse.PropertySummary toSummary(
            Property property, SettlementPlan settlementPlan) {
        String thumbnailUrl = property.getImages().isEmpty()
                ? null
                : property.getImages().getFirst().getStorageKey();
        PropertyDetailResponse.Simulation simulation = createSimulation(property, settlementPlan);

        return new PropertyListResponse.PropertySummary(
                property.getId(),
                property.getPropertyName(),
                property.getRent(),
                property.getConfirmedInitialCost(),
                simulation == null ? null : simulation.livingMonths(),
                property.getPriorityRank(),
                thumbnailUrl);
    }

    private record PropertyDetailCostPresentation(
            PropertyDetailResponse.CostSummary summary,
            PropertyDetailResponse.CostGroups costGroups,
            List<PropertyDetailResponse.ExcludedCost> excludedCosts) {
    }

}
