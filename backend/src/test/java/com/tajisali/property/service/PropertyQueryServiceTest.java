package com.tajisali.property.service;

import com.tajisali.property.domain.Property;
import com.tajisali.property.domain.PropertyAiAnalysis;
import com.tajisali.property.repository.PropertyAiAnalysisRepository;
import com.tajisali.property.repository.PropertyRepository;
import com.tajisali.settlement.domain.CostCategory;
import com.tajisali.settlement.domain.CostType;
import com.tajisali.settlement.domain.CurrencyCode;
import com.tajisali.settlement.domain.MonthlyLivingCostInputMethod;
import com.tajisali.settlement.domain.SettlementPlan;
import com.tajisali.settlement.domain.SettlementPlanCostItem;
import com.tajisali.settlement.repository.SettlementPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyQueryServiceTest {

    @Mock PropertyRepository propertyRepository;
    @Mock PropertyAiAnalysisRepository propertyAiAnalysisRepository;
    @Mock SettlementPlanRepository settlementPlanRepository;
    private PropertyQueryService propertyQueryService;

    @BeforeEach
    void setUp() {
        propertyQueryService = new PropertyQueryService(
                propertyRepository, propertyAiAnalysisRepository, settlementPlanRepository);
    }

    @Test
    void 최신순_매물의_화면용_요약정보를_반환한다() {
        Property property = new Property(
                "요코하마 스튜디오", 65_000L, 245_000L, 70_000L,
                1, LocalDateTime.now());
        ReflectionTestUtils.setField(property, "id", 10L);

        when(propertyRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(property));
        SettlementPlan plan = new SettlementPlan(
                LocalDate.now().plusMonths(1), 12,
                8_000_000L, 100_000L, 1_000_000L, 0L,
                MonthlyLivingCostInputMethod.DEFAULT);
        plan.addCostItem(new SettlementPlanCostItem(
                CostCategory.INITIAL, CostType.AIRFARE, 62_000L, CurrencyCode.JPY));
        plan.addCostItem(new SettlementPlanCostItem(
                CostCategory.MONTHLY, CostType.FOOD, 115_000L, CurrencyCode.JPY));
        when(settlementPlanRepository.findTopByOrderByCreatedAtDesc())
                .thenReturn(Optional.of(plan));
        when(propertyAiAnalysisRepository.findTopByPropertyIdOrderByIdDesc(10L))
                .thenReturn(Optional.empty());

        var response = propertyQueryService.getProperties();

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.properties().getFirst())
                .extracting(
                        item -> item.propertyId(),
                        item -> item.propertyName(),
                        item -> item.rent(),
                        item -> item.initialCost(),
                        item -> item.livingMonths(),
                        item -> item.priorityRank())
                .containsExactly(
                        10L, "요코하마 스튜디오", 65_000L, 245_000L,
                        new java.math.BigDecimal("3.2"),
                        1);
    }

    @Test
    void 매물이_없으면_빈_목록을_반환한다() {
        when(settlementPlanRepository.findTopByOrderByCreatedAtDesc())
                .thenReturn(Optional.empty());
        when(propertyRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        var response = propertyQueryService.getProperties();

        assertThat(response.totalCount()).isZero();
        assertThat(response.properties()).isEmpty();
    }

    @Test
    void 분석_ID로_비용과_자금_시뮬레이션이_포함된_상세를_조회한다() {
        Property property = new Property(
                "요코하마 스튜디오", 65_000L, 245_000L, 70_000L,
                1, LocalDateTime.now());
        ReflectionTestUtils.setField(property, "id", 10L);
        ReflectionTestUtils.setField(property, "deposit", 65_000L);

        PropertyAiAnalysis analysis = org.springframework.beans.BeanUtils
                .instantiateClass(PropertyAiAnalysis.class);
        ReflectionTestUtils.setField(analysis, "id", 1L);
        ReflectionTestUtils.setField(analysis, "property", property);

        SettlementPlan plan = new SettlementPlan(
                LocalDate.now().plusMonths(1), 12,
                8_000_000L, 100_000L, 1_000_000L, 0L,
                MonthlyLivingCostInputMethod.DEFAULT);
        ReflectionTestUtils.setField(plan, "id", 7L);
        plan.addCostItem(new SettlementPlanCostItem(
                CostCategory.INITIAL, CostType.AIRFARE, 62_000L, CurrencyCode.JPY));
        plan.addCostItem(new SettlementPlanCostItem(
                CostCategory.MONTHLY, CostType.FOOD, 115_000L, CurrencyCode.JPY));

        when(propertyAiAnalysisRepository.findDetailById(1L))
                .thenReturn(Optional.of(analysis));
        when(settlementPlanRepository.findTopByOrderByCreatedAtDesc())
                .thenReturn(Optional.of(plan));

        var response = propertyQueryService.getPropertyDetail(1L);

        assertThat(response.propertyId()).isEqualTo(10L);
        assertThat(response.analysisId()).isEqualTo(1L);
        assertThat(response.settlementPlanId()).isEqualTo(7L);
        assertThat(response.costAnalysis().refundableAmount()).isEqualTo(65_000L);
        assertThat(response.costAnalysis().nonRefundableAmount()).isEqualTo(180_000L);
        assertThat(response.simulation().exchangeRate().jpy()).isEqualTo(100);
        assertThat(response.simulation().exchangeRate().krw()).isEqualTo(860);
        assertThat(response.simulation().livingMonths()).isEqualByComparingTo("3.2");
    }
}
