package com.tajisali.property.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.exchange.service.ExchangeRateService;
import com.tajisali.property.domain.CostTiming;
import com.tajisali.property.domain.ObligationStatus;
import com.tajisali.property.domain.Property;
import com.tajisali.property.domain.PropertyCostItem;
import com.tajisali.property.dto.PropertyComparisonRequest;
import com.tajisali.property.dto.PropertyComparisonResponse;
import com.tajisali.property.repository.PropertyRepository;
import com.tajisali.settlement.domain.MonthlyLivingCostInputMethod;
import com.tajisali.settlement.domain.SettlementPlan;
import com.tajisali.settlement.repository.SettlementPlanRepository;
import com.tajisali.user.domain.User;
import com.tajisali.user.service.AnonymousUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PropertyComparisonService {

    private final AnonymousUserService anonymousUserService;
    private final PropertyRepository propertyRepository;
    private final SettlementPlanRepository settlementPlanRepository;
    private final PropertyCostCalculationService propertyCostCalculationService;
    private final PropertyFundSimulationService propertyFundSimulationService;

    @Transactional
    public PropertyComparisonResponse compare(
            PropertyComparisonRequest request,
            String userKey) {
        validatePropertyIds(request.propertyIds());

        User user = anonymousUserService.findExisting(userKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));
        SettlementPlan settlementPlan = settlementPlanRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_PLAN_NOT_FOUND));

        Map<Long, Property> propertiesById = findOwnedProperties(
                request.propertyIds(), user.getId());
        List<ComparisonSource> sources = request.propertyIds().stream()
                .map(propertiesById::get)
                .map(property -> createSource(property, settlementPlan))
                .toList();

        HighlightIds highlights = findHighlightIds(sources);
        user.markPropertiesCompared();

        return new PropertyComparisonResponse(
                settlementPlan.getId(),
                new PropertyComparisonResponse.ExchangeRate(
                        Math.toIntExact(ExchangeRateService.BASE_JPY),
                        Math.toIntExact(ExchangeRateService.KRW_PER_100_JPY)),
                sources.stream()
                        .map(source -> toResponse(source, highlights))
                        .toList());
    }

    private void validatePropertyIds(List<Long> propertyIds) {
        if (propertyIds == null || propertyIds.size() < 2 || propertyIds.size() > 3) {
            throw new BusinessException(ErrorCode.PROPERTY_COMPARISON_INVALID_SIZE);
        }
        if (propertyIds.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
        }
        Set<Long> uniqueIds = new HashSet<>(propertyIds);
        if (uniqueIds.size() != propertyIds.size()) {
            throw new BusinessException(ErrorCode.PROPERTY_COMPARISON_DUPLICATE_PROPERTY);
        }
    }

    private Map<Long, Property> findOwnedProperties(List<Long> propertyIds, Long userId) {
        List<Property> properties = propertyRepository.findAllByIdInAndUserId(propertyIds, userId);
        if (properties.size() != propertyIds.size()) {
            throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
        }

        Map<Long, Property> propertiesById = new HashMap<>();
        properties.forEach(property -> propertiesById.put(property.getId(), property));
        return propertiesById;
    }

    private ComparisonSource createSource(Property property, SettlementPlan settlementPlan) {
        PropertyCostCalculationResult costCalculation = propertyCostCalculationService.calculate(property);
        PropertyFundSimulationResult simulation = createSimulation(property, settlementPlan);
        int unknownCostItemCount = countUnknownCostItems(property);

        return new ComparisonSource(
                property,
                costCalculation,
                simulation,
                unknownCostItemCount,
                unknownCostItemCount > 0);
    }

    private PropertyFundSimulationResult createSimulation(
            Property property,
            SettlementPlan settlementPlan) {
        if (settlementPlan.getMonthlyLivingCostInputMethod() != MonthlyLivingCostInputMethod.DIRECT
                || property.getConfirmedInitialCost() == null
                || property.getConfirmedMonthlyCost() == null) {
            return null;
        }
        return propertyFundSimulationService.calculate(
                property.getConfirmedInitialCost(),
                property.getConfirmedMonthlyCost(),
                settlementPlan);
    }

    private int countUnknownCostItems(Property property) {
        int count = 0;
        if (property.getDeposit() == null) {
            count++;
        }
        if (property.getKeyMoney() == null) {
            count++;
        }
        if (property.getRent() == null) {
            count++;
        }
        if (property.getManagementFee() == null) {
            count++;
        }
        for (PropertyCostItem item : property.getCostItems()) {
            if (item.getAmount() == null
                    || item.getObligationStatus() == ObligationStatus.UNKNOWN
                    || item.getTiming() == CostTiming.UNKNOWN) {
                count++;
            }
        }
        return count;
    }

    private HighlightIds findHighlightIds(List<ComparisonSource> sources) {
        List<ComparisonSource> candidates = sources.stream()
                .filter(source -> !source.hasUnknownCosts())
                .filter(source -> source.simulation() != null)
                .toList();

        return new HighlightIds(
                findLowestInitialSettlementCostIds(candidates),
                findLowestMonthlyHousingCostIds(candidates),
                findLongestLivingMonthsIds(candidates));
    }

    private Set<Long> findLowestInitialSettlementCostIds(List<ComparisonSource> candidates) {
        return findLowestIds(candidates, source -> source.simulation().initialCost());
    }

    private Set<Long> findLowestMonthlyHousingCostIds(List<ComparisonSource> candidates) {
        return findLowestIds(candidates, source -> source.simulation().monthlyHousingCost());
    }

    private Set<Long> findLowestIds(
            List<ComparisonSource> candidates,
            java.util.function.ToLongFunction<ComparisonSource> valueExtractor) {
        if (candidates.isEmpty()) {
            return Set.of();
        }
        long lowestValue = candidates.stream()
                .mapToLong(valueExtractor)
                .min()
                .orElseThrow();
        return candidates.stream()
                .filter(source -> valueExtractor.applyAsLong(source) == lowestValue)
                .map(source -> source.property().getId())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private Set<Long> findLongestLivingMonthsIds(List<ComparisonSource> candidates) {
        List<ComparisonSource> unlimitedCandidates = candidates.stream()
                .filter(source -> source.simulation().isUnlimited())
                .toList();
        if (!unlimitedCandidates.isEmpty()) {
            return unlimitedCandidates.stream()
                    .map(source -> source.property().getId())
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }

        List<ComparisonSource> finiteCandidates = candidates.stream()
                .filter(source -> source.simulation().livingMonths() != null)
                .toList();
        if (finiteCandidates.isEmpty()) {
            return Set.of();
        }
        BigDecimal longestLivingMonths = finiteCandidates.stream()
                .map(source -> source.simulation().livingMonths())
                .max(BigDecimal::compareTo)
                .orElseThrow();
        return finiteCandidates.stream()
                .filter(source -> source.simulation().livingMonths().compareTo(longestLivingMonths) == 0)
                .map(source -> source.property().getId())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private PropertyComparisonResponse.PropertyComparison toResponse(
            ComparisonSource source,
            HighlightIds highlightIds) {
        Property property = source.property();
        PropertyFundSimulationResult simulation = source.simulation();

        return new PropertyComparisonResponse.PropertyComparison(
                property.getId(),
                property.getPropertyName(),
                property.getImages().isEmpty() ? null : property.getImages().getFirst().getStorageKey(),
                property.getPriorityRank(),
                new PropertyComparisonResponse.Conditions(
                        property.getPrefecture(),
                        property.getCity(),
                        property.getNearestStation(),
                        property.getWalkMinutes(),
                        property.getExclusiveAreaM2(),
                        property.getContractPeriodMonths()),
                new PropertyComparisonResponse.Costs(
                        property.getConfirmedInitialCost(),
                        property.getConfirmedMonthlyCost(),
                        simulation == null ? null : simulation.initialCost(),
                        source.costCalculation().refundableAmount(),
                        source.costCalculation().nonRefundableAmount(),
                        calculatePlannedStayHousingCost(property, simulation),
                        source.unknownCostItemCount(),
                        source.hasUnknownCosts()),
                toSimulationResponse(simulation),
                new PropertyComparisonResponse.Highlights(
                        highlightIds.lowestInitialSettlementCostIds().contains(property.getId()),
                        highlightIds.lowestMonthlyHousingCostIds().contains(property.getId()),
                        highlightIds.longestLivingMonthsIds().contains(property.getId())));
    }

    private Long calculatePlannedStayHousingCost(
            Property property,
            PropertyFundSimulationResult simulation) {
        if (simulation == null || property.getConfirmedMonthlyCost() == null) {
            return null;
        }
        try {
            return Math.multiplyExact(
                    property.getConfirmedMonthlyCost(),
                    (long) simulation.plannedStayMonths());
        } catch (ArithmeticException exception) {
            throw new BusinessException(ErrorCode.PROPERTY_FUND_SIMULATION_TOTAL_OVERFLOW, exception);
        }
    }

    private PropertyComparisonResponse.Simulation toSimulationResponse(
            PropertyFundSimulationResult simulation) {
        if (simulation == null) {
            return null;
        }
        return new PropertyComparisonResponse.Simulation(
                simulation.canMoveIn(),
                simulation.balanceAfterMoveIn(),
                simulation.monthlyHousingCost(),
                simulation.monthlyLivingCost(),
                simulation.totalMonthlyCost(),
                simulation.monthlyBalances().stream()
                        .map(balance -> new PropertyComparisonResponse.MonthlyBalance(
                                balance.month(), balance.balance()))
                        .toList(),
                simulation.livingMonths(),
                simulation.isUnlimited(),
                simulation.requiredFunds(),
                simulation.surplus(),
                simulation.shortageJpy(),
                simulation.shortageKrw());
    }

    private record ComparisonSource(
            Property property,
            PropertyCostCalculationResult costCalculation,
            PropertyFundSimulationResult simulation,
            int unknownCostItemCount,
            boolean hasUnknownCosts) {
    }

    private record HighlightIds(
            Set<Long> lowestInitialSettlementCostIds,
            Set<Long> lowestMonthlyHousingCostIds,
            Set<Long> longestLivingMonthsIds) {
    }
}
