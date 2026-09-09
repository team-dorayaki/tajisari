package com.tajisali.property.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.property.domain.CostTiming;
import com.tajisali.property.domain.ObligationStatus;
import com.tajisali.property.domain.Property;
import com.tajisali.property.domain.PropertyCostItem;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyCostCalculationServiceTest {

    private final PropertyCostCalculationService calculationService =
            new PropertyCostCalculationService();

    @Test
    void 필수_비용과_사용자가_선택한_선택_비용만_초기비용과_월주거비에_포함한다() {
        Property property = property(70_000L, 5_000L, 100_000L, 20_000L);
        setCostItems(property, List.of(
                costItem(10_000L, ObligationStatus.REQUIRED, false, CostTiming.INITIAL),
                costItem(2_000L, ObligationStatus.OPTIONAL, true, CostTiming.INITIAL),
                costItem(9_000L, ObligationStatus.OPTIONAL, false, CostTiming.INITIAL),
                costItem(1_000L, ObligationStatus.REQUIRED, false, CostTiming.MONTHLY),
                costItem(2_000L, ObligationStatus.OPTIONAL, true, CostTiming.MONTHLY),
                costItem(9_000L, ObligationStatus.OPTIONAL, false, CostTiming.MONTHLY)));

        PropertyCostCalculationResult result = calculationService.calculate(property);

        assertThat(result.initialCost()).isEqualTo(132_000L);
        assertThat(result.monthlyCost()).isEqualTo(78_000L);
        assertThat(result.refundableAmount()).isEqualTo(100_000L);
        assertThat(result.nonRefundableAmount()).isEqualTo(32_000L);
        assertThat(result.hasUnknownInitialCosts()).isFalse();
        assertThat(result.hasUnknownMonthlyCosts()).isFalse();
        assertThat(result.hasUnclassifiedCosts()).isFalse();
    }

    @Test
    void 확정된_0엔은_미확인이_아닌_계산_값으로_처리한다() {
        Property property = property(0L, 0L, 0L, 0L);

        PropertyCostCalculationResult result = calculationService.calculate(property);

        assertThat(result.initialCost()).isZero();
        assertThat(result.monthlyCost()).isZero();
        assertThat(result.refundableAmount()).isZero();
        assertThat(result.nonRefundableAmount()).isZero();
        assertThat(result.hasUnknownInitialCosts()).isFalse();
        assertThat(result.hasUnknownMonthlyCosts()).isFalse();
    }

    @Test
    void 미확인_비용은_최소_합계에서_제외하고_미확인_상태를_반환한다() {
        Property property = property(70_000L, null, 100_000L, null);
        setCostItems(property, List.of(
                costItem(null, ObligationStatus.REQUIRED, false, CostTiming.INITIAL),
                costItem(50_000L, ObligationStatus.UNKNOWN, false, CostTiming.INITIAL),
                costItem(null, ObligationStatus.REQUIRED, false, CostTiming.MONTHLY),
                costItem(3_000L, ObligationStatus.UNKNOWN, false, CostTiming.MONTHLY)));

        PropertyCostCalculationResult result = calculationService.calculate(property);

        assertThat(result.initialCost()).isEqualTo(100_000L);
        assertThat(result.monthlyCost()).isEqualTo(70_000L);
        assertThat(result.refundableAmount()).isEqualTo(100_000L);
        assertThat(result.nonRefundableAmount()).isNull();
        assertThat(result.hasUnknownInitialCosts()).isTrue();
        assertThat(result.hasUnknownMonthlyCosts()).isTrue();
    }

    @Test
    void 갱신_퇴거_조건부_비용은_계산에서_제외하고_발생시점_미확인_비용은_별도로_표시한다() {
        Property property = property(70_000L, 5_000L, 100_000L, 20_000L);
        setCostItems(property, List.of(
                costItem(10_000L, ObligationStatus.REQUIRED, false, CostTiming.RENEWAL),
                costItem(20_000L, ObligationStatus.REQUIRED, false, CostTiming.MOVE_OUT),
                costItem(30_000L, ObligationStatus.REQUIRED, false, CostTiming.CONDITIONAL),
                costItem(40_000L, ObligationStatus.REQUIRED, false, CostTiming.UNKNOWN)));

        PropertyCostCalculationResult result = calculationService.calculate(property);

        assertThat(result.initialCost()).isEqualTo(120_000L);
        assertThat(result.monthlyCost()).isEqualTo(75_000L);
        assertThat(result.hasUnknownInitialCosts()).isFalse();
        assertThat(result.hasUnknownMonthlyCosts()).isFalse();
        assertThat(result.hasUnclassifiedCosts()).isTrue();
    }

    @Test
    void 모든_관련_비용이_미확인이면_합계를_null로_반환한다() {
        Property property = property(null, null, null, null);

        PropertyCostCalculationResult result = calculationService.calculate(property);

        assertThat(result.initialCost()).isNull();
        assertThat(result.monthlyCost()).isNull();
        assertThat(result.refundableAmount()).isNull();
        assertThat(result.nonRefundableAmount()).isNull();
        assertThat(result.hasUnknownInitialCosts()).isTrue();
        assertThat(result.hasUnknownMonthlyCosts()).isTrue();
    }

    @Test
    void 비용_합계가_long_범위를_초과하면_오류를_반환한다() {
        Property property = property(Long.MAX_VALUE, 0L, 0L, 0L);
        setCostItems(property, List.of(
                costItem(1L, ObligationStatus.REQUIRED, false, CostTiming.MONTHLY)));

        assertThatThrownBy(() -> calculationService.calculate(property))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PROPERTY_COST_TOTAL_OVERFLOW);
    }

    private Property property(Long rent, Long managementFee, Long deposit, Long keyMoney) {
        Property property = new Property(
                "테스트 매물", rent, null, null, null, LocalDateTime.now());
        ReflectionTestUtils.setField(property, "managementFee", managementFee);
        ReflectionTestUtils.setField(property, "deposit", deposit);
        ReflectionTestUtils.setField(property, "keyMoney", keyMoney);
        return property;
    }

    private PropertyCostItem costItem(
            Long amount,
            ObligationStatus obligationStatus,
            boolean includedInCalculation,
            CostTiming timing) {
        PropertyCostItem item = org.springframework.beans.BeanUtils
                .instantiateClass(PropertyCostItem.class);
        ReflectionTestUtils.setField(item, "amount", amount);
        ReflectionTestUtils.setField(item, "obligationStatus", obligationStatus);
        ReflectionTestUtils.setField(item, "includedInCalculation", includedInCalculation);
        ReflectionTestUtils.setField(item, "timing", timing);
        return item;
    }

    private void setCostItems(Property property, List<PropertyCostItem> costItems) {
        ReflectionTestUtils.setField(property, "costItems", costItems);
    }
}
