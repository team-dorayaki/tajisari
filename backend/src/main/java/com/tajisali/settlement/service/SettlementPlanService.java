package com.tajisali.settlement.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.settlement.domain.*;
import com.tajisali.settlement.dto.CurrencyTotalsResponse;
import com.tajisali.settlement.dto.SettlementPlanCostItemRequest;
import com.tajisali.settlement.dto.SettlementPlanCreateRequest;
import com.tajisali.settlement.dto.SettlementPlanCreateResponse;
import com.tajisali.settlement.repository.SettlementPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SettlementPlanService {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final SettlementPlanRepository settlementPlanRepository;

    @Transactional
    public SettlementPlanCreateResponse create(SettlementPlanCreateRequest request) {
        // 비즈니스 규칙 검증
        validateRequest(request);

        // 비용 구분·통화별 합계 계산
        CurrencyTotalsResponse additionalInitialCostTotals = calculateTotals(request.getAdditionalInitialCosts());
        CurrencyTotalsResponse monthlyLivingCostTotals = calculateTotals(request.getMonthlyLivingCosts());

        // 정착 계획과 비용 항목 생성
        SettlementPlan settlementPlan = createSettlementPlan(request);
        addCostItems(settlementPlan, request.getAdditionalInitialCosts(), CostCategory.INITIAL);
        addCostItems(settlementPlan, request.getMonthlyLivingCosts(), CostCategory.MONTHLY);

        SettlementPlan savedPlan = settlementPlanRepository.save(settlementPlan);
        return new SettlementPlanCreateResponse(
                savedPlan.getId(), additionalInitialCostTotals, monthlyLivingCostTotals, "SAVED");
    }

    private void validateRequest(SettlementPlanCreateRequest request) {
        validateMoveInDate(request.getMoveInDate());
        validateEmergencyReserve(request);
        validateCostItems(request.getAdditionalInitialCosts(), CostCategory.INITIAL);
        validateCostItems(request.getMonthlyLivingCosts(), CostCategory.MONTHLY);
    }

    private void validateMoveInDate(LocalDate moveInDate) {
        if (!moveInDate.isAfter(LocalDate.now(KOREA_ZONE_ID))) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
        }
    }

    private void validateEmergencyReserve(SettlementPlanCreateRequest request) {
        if (request.getEmergencyReserve().getKrw() > request.getPreparedFunds().getKrw()
                || request.getEmergencyReserve().getJpy() > request.getPreparedFunds().getJpy()) {
            throw new BusinessException(ErrorCode.SETTLEMENT_PLAN_RESERVE_EXCEEDS_FUNDS);
        }
    }

    private void validateCostItems(
            List<SettlementPlanCostItemRequest> costItems, CostCategory costCategory) {
        Set<CostType> costTypes = new HashSet<>();
        for (SettlementPlanCostItemRequest costItem : costItems) {
            if (!isAllowedCostType(costCategory, costItem.getType())) {
                throw new BusinessException(ErrorCode.SETTLEMENT_PLAN_INVALID_COST_TYPE);
            }
            if (!costTypes.add(costItem.getType())) {
                throw new BusinessException(ErrorCode.SETTLEMENT_PLAN_DUPLICATE_COST_TYPE);
            }
        }
    }

    private boolean isAllowedCostType(CostCategory costCategory, CostType costType) {
        if (costCategory == CostCategory.INITIAL) {
            return costType == CostType.AIRFARE
                    || costType == CostType.MOVING
                    || costType == CostType.FURNITURE_APPLIANCE
                    || costType == CostType.VISA_ADMINISTRATION
                    || costType == CostType.OTHER;
        }
        return costType == CostType.FOOD
                || costType == CostType.TRANSPORTATION
                || costType == CostType.UTILITIES
                || costType == CostType.COMMUNICATION
                || costType == CostType.INSURANCE_TAX
                || costType == CostType.OTHER;
    }

    private CurrencyTotalsResponse calculateTotals(List<SettlementPlanCostItemRequest> costItems) {
        long krw = 0;
        long jpy = 0;
        for (SettlementPlanCostItemRequest costItem : costItems) {
            if (costItem.getCurrency() == CurrencyCode.KRW) {
                krw = addAmount(krw, costItem.getAmount());
            } else {
                jpy = addAmount(jpy, costItem.getAmount());
            }
        }
        return new CurrencyTotalsResponse(krw, jpy);
    }

    private long addAmount(long total, long amount) {
        try {
            return Math.addExact(total, amount);
        } catch (ArithmeticException exception) {
            throw new BusinessException(ErrorCode.SETTLEMENT_PLAN_COST_TOTAL_OVERFLOW, exception);
        }
    }

    private SettlementPlan createSettlementPlan(SettlementPlanCreateRequest request) {
        return new SettlementPlan(
                request.getMoveInDate(),
                request.getPlannedStayMonths(),
                request.getPreparedFunds().getKrw(),
                request.getPreparedFunds().getJpy(),
                request.getEmergencyReserve().getKrw(),
                request.getEmergencyReserve().getJpy(),
                request.getMonthlyLivingCostInputMethod());
    }

    private void addCostItems(
            SettlementPlan settlementPlan,
            List<SettlementPlanCostItemRequest> costItems,
            CostCategory costCategory) {
        for (SettlementPlanCostItemRequest costItem : costItems) {
            settlementPlan.addCostItem(new SettlementPlanCostItem(
                    costCategory, costItem.getType(), costItem.getAmount(), costItem.getCurrency()));
        }
    }
}
