package com.tajisali.property.service;

import com.tajisali.exchange.service.ExchangeRateService;
import com.tajisali.property.domain.Property;
import com.tajisali.property.domain.CostTiming;
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
                        property.getConfirmedInitialCost(),
                        property.getConfirmedMonthlyCost(),
                        costCalculation.refundableAmount(),
                        costCalculation.nonRefundableAmount(),
                        costCalculation.hasUnknownInitialCosts(),
                        costCalculation.hasUnknownMonthlyCosts(),
                        costCalculation.hasUnclassifiedCosts(),
                        property.getCostItems().stream().map(this::toCostItem).toList()),
                createSimulation(property, plan));
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

}
