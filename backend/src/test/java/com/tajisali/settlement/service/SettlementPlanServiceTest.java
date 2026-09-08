package com.tajisali.settlement.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.settlement.domain.*;
import com.tajisali.settlement.dto.CurrencyAmountsRequest;
import com.tajisali.settlement.dto.SettlementPlanCostItemRequest;
import com.tajisali.settlement.dto.SettlementPlanCreateRequest;
import com.tajisali.settlement.repository.SettlementPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementPlanServiceTest {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Mock
    private SettlementPlanRepository settlementPlanRepository;

    private SettlementPlanService settlementPlanService;

    @BeforeEach
    void setUp() {
        settlementPlanService = new SettlementPlanService(settlementPlanRepository);
    }

    @Test
    void 정상_요청은_비용_항목과_합계를_저장_응답에_반영한다() {
        SettlementPlanCreateRequest request = request(
                List.of(cost(CostType.AIRFARE, 40_000, CurrencyCode.JPY),
                        cost(CostType.MOVING, 22_000, CurrencyCode.JPY)),
                List.of(cost(CostType.FOOD, 40_000, CurrencyCode.JPY),
                        cost(CostType.TRANSPORTATION, 10_000, CurrencyCode.JPY),
                        cost(CostType.UTILITIES, 12_000, CurrencyCode.JPY),
                        cost(CostType.COMMUNICATION, 8_000, CurrencyCode.JPY),
                        cost(CostType.INSURANCE_TAX, 25_000, CurrencyCode.JPY),
                        cost(CostType.OTHER, 20_000, CurrencyCode.JPY)));
        saveWithId(1L);

        var response = settlementPlanService.create(request);

        assertThat(response.getPlanId()).isEqualTo(1L);
        assertThat(response.getAdditionalInitialCostTotals().getJpy()).isEqualTo(62_000L);
        assertThat(response.getMonthlyLivingCostTotals().getJpy()).isEqualTo(115_000L);
        assertThat(response.getStatus()).isEqualTo("SAVED");

        ArgumentCaptor<SettlementPlan> planCaptor = ArgumentCaptor.forClass(SettlementPlan.class);
        verify(settlementPlanRepository).save(planCaptor.capture());
        SettlementPlan savedPlan = planCaptor.getValue();
        assertThat(savedPlan.getMoveInDate()).isEqualTo(request.getMoveInDate());
        assertThat(savedPlan.getPreparedFundsKrw()).isEqualTo(1_000_000L);
        assertThat(savedPlan.getPreparedFundsJpy()).isEqualTo(100_000L);
        assertThat(savedPlan.getMonthlyLivingCostInputMethod()).isEqualTo(MonthlyLivingCostInputMethod.DEFAULT);
        assertThat(savedPlan.getCostItems()).hasSize(8);
        assertThat(savedPlan.getCostItems().getFirst())
                .extracting(SettlementPlanCostItem::getCostCategory,
                        SettlementPlanCostItem::getCostType,
                        SettlementPlanCostItem::getAmount,
                        SettlementPlanCostItem::getCurrency)
                .containsExactly(CostCategory.INITIAL, CostType.AIRFARE, 40_000L, CurrencyCode.JPY);
        assertThat(savedPlan.getCostItems().get(2))
                .extracting(SettlementPlanCostItem::getCostCategory,
                        SettlementPlanCostItem::getCostType,
                        SettlementPlanCostItem::getAmount,
                        SettlementPlanCostItem::getCurrency)
                .containsExactly(CostCategory.MONTHLY, CostType.FOOD, 40_000L, CurrencyCode.JPY);
        assertThat(savedPlan.getCostItems().getFirst().getSettlementPlan()).isSameAs(savedPlan);
    }

    @Test
    void 통화별_비용은_환산없이_독립적으로_합산한다() {
        SettlementPlanCreateRequest request = request(
                List.of(cost(CostType.AIRFARE, 1_000, CurrencyCode.KRW),
                        cost(CostType.MOVING, 2_000, CurrencyCode.JPY)),
                List.of(cost(CostType.FOOD, 3_000, CurrencyCode.KRW),
                        cost(CostType.TRANSPORTATION, 4_000, CurrencyCode.JPY)));
        saveWithId(1L);

        var response = settlementPlanService.create(request);

        assertThat(response.getAdditionalInitialCostTotals().getKrw()).isEqualTo(1_000L);
        assertThat(response.getAdditionalInitialCostTotals().getJpy()).isEqualTo(2_000L);
        assertThat(response.getMonthlyLivingCostTotals().getKrw()).isEqualTo(3_000L);
        assertThat(response.getMonthlyLivingCostTotals().getJpy()).isEqualTo(4_000L);
    }

    @Test
    void 빈_목록과_0원_비용_항목도_저장한다() {
        SettlementPlanCreateRequest request = request(
                List.of(), List.of(cost(CostType.FOOD, 0, CurrencyCode.KRW)));
        saveWithId(1L);

        var response = settlementPlanService.create(request);

        assertThat(response.getAdditionalInitialCostTotals().getKrw()).isZero();
        assertThat(response.getAdditionalInitialCostTotals().getJpy()).isZero();
        assertThat(response.getMonthlyLivingCostTotals().getKrw()).isZero();
        assertThat(response.getMonthlyLivingCostTotals().getJpy()).isZero();

        ArgumentCaptor<SettlementPlan> planCaptor = ArgumentCaptor.forClass(SettlementPlan.class);
        verify(settlementPlanRepository).save(planCaptor.capture());
        assertThat(planCaptor.getValue().getCostItems()).singleElement()
                .extracting(SettlementPlanCostItem::getAmount)
                .isEqualTo(0L);
    }

    @Test
    void 입주일이_오늘이면_거부한다() {
        SettlementPlanCreateRequest request = request(List.of(), List.of());
        request.setMoveInDate(LocalDate.now(KOREA_ZONE_ID));

        assertErrorCode(request, ErrorCode.COMMON_INVALID_INPUT);
    }

    @Test
    void 입주일이_과거이면_거부한다() {
        SettlementPlanCreateRequest request = request(List.of(), List.of());
        request.setMoveInDate(LocalDate.now(KOREA_ZONE_ID).minusDays(1));

        assertErrorCode(request, ErrorCode.COMMON_INVALID_INPUT);
    }

    @Test
    void 입주일이_내일이면_저장한다() {
        SettlementPlanCreateRequest request = request(List.of(), List.of());
        saveWithId(1L);

        var response = settlementPlanService.create(request);

        assertThat(response.getPlanId()).isEqualTo(1L);
        verify(settlementPlanRepository).save(any(SettlementPlan.class));
    }

    @Test
    void 비상예비비가_같은_통화의_준비자금을_초과하면_거부한다() {
        SettlementPlanCreateRequest request = request(List.of(), List.of());
        request.setEmergencyReserve(new CurrencyAmountsRequest(1_000_001L, 10_000L));

        assertErrorCode(request, ErrorCode.SETTLEMENT_PLAN_RESERVE_EXCEEDS_FUNDS);
    }

    @Test
    void 비용_종류가_목록_카테고리와_호환되지_않으면_거부한다() {
        SettlementPlanCreateRequest request = request(
                List.of(cost(CostType.FOOD, 1, CurrencyCode.KRW)), List.of());

        assertErrorCode(request, ErrorCode.SETTLEMENT_PLAN_INVALID_COST_TYPE);
    }

    @Test
    void 통화가_달라도_같은_카테고리의_비용_종류가_중복되면_거부한다() {
        SettlementPlanCreateRequest request = request(
                List.of(cost(CostType.AIRFARE, 1, CurrencyCode.KRW),
                        cost(CostType.AIRFARE, 1, CurrencyCode.JPY)),
                List.of());

        assertErrorCode(request, ErrorCode.SETTLEMENT_PLAN_DUPLICATE_COST_TYPE);
    }

    @Test
    void 양쪽_카테고리의_OTHER는_각각_허용한다() {
        SettlementPlanCreateRequest request = request(
                List.of(cost(CostType.OTHER, 1, CurrencyCode.KRW)),
                List.of(cost(CostType.OTHER, 1, CurrencyCode.JPY)));
        saveWithId(1L);

        var response = settlementPlanService.create(request);

        assertThat(response.getPlanId()).isEqualTo(1L);
        verify(settlementPlanRepository).save(any(SettlementPlan.class));
    }

    @Test
    void 비용_합계가_오버플로되면_거부한다() {
        SettlementPlanCreateRequest request = request(
                List.of(cost(CostType.AIRFARE, Long.MAX_VALUE, CurrencyCode.KRW),
                        cost(CostType.MOVING, 1, CurrencyCode.KRW)),
                List.of());

        assertErrorCode(request, ErrorCode.SETTLEMENT_PLAN_COST_TOTAL_OVERFLOW);
    }

    @Test
    void 조회는_저장된_기본정보와_비용을_카테고리별로_반환한다() {
        SettlementPlan plan = storedPlan();
        plan.addCostItem(new SettlementPlanCostItem(CostCategory.INITIAL, CostType.OTHER, 20, CurrencyCode.JPY));
        plan.addCostItem(new SettlementPlanCostItem(CostCategory.MONTHLY, CostType.OTHER, 30, CurrencyCode.KRW));
        when(settlementPlanRepository.findById(42L)).thenReturn(Optional.of(plan));

        var response = settlementPlanService.get(42L);

        assertThat(response.getPlanId()).isEqualTo(42L);
        assertThat(response.getMoveInDate()).isEqualTo(plan.getMoveInDate());
        assertThat(response.getPlannedStayMonths()).isEqualTo(12);
        assertThat(response.getPreparedFunds().getKrw()).isEqualTo(1_000_000L);
        assertThat(response.getPreparedFunds().getJpy()).isEqualTo(100_000L);
        assertThat(response.getEmergencyReserve().getKrw()).isEqualTo(100_000L);
        assertThat(response.getEmergencyReserve().getJpy()).isEqualTo(10_000L);
        assertThat(response.getMonthlyLivingCostInputMethod()).isEqualTo(MonthlyLivingCostInputMethod.DEFAULT);
        assertThat(response.getAdditionalInitialCosts()).extracting(item -> item.type())
                .containsExactly(CostType.AIRFARE, CostType.OTHER);
        assertThat(response.getMonthlyLivingCosts()).extracting(item -> item.type())
                .containsExactly(CostType.FOOD, CostType.OTHER);
        assertThat(response.getAdditionalInitialCosts().getFirst().amount()).isEqualTo(1_000L);
        assertThat(response.getAdditionalInitialCosts().getFirst().currency()).isEqualTo(CurrencyCode.KRW);
        assertThat(response.getAdditionalInitialCostTotals().getKrw()).isEqualTo(1_000L);
        assertThat(response.getAdditionalInitialCostTotals().getJpy()).isEqualTo(20L);
        assertThat(response.getMonthlyLivingCostTotals().getKrw()).isEqualTo(30L);
        assertThat(response.getMonthlyLivingCostTotals().getJpy()).isEqualTo(2_000L);
        verify(settlementPlanRepository).findById(42L);
        verifyNoMoreInteractions(settlementPlanRepository);
    }

    @Test
    void 없는_계획은_조회와_수정에서_404_오류를_발생시킨다() {
        when(settlementPlanRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settlementPlanService.get(42L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_PLAN_NOT_FOUND);
        assertThatThrownBy(() -> settlementPlanService.update(42L, request(List.of(), List.of())))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_PLAN_NOT_FOUND);
        verify(settlementPlanRepository, never()).save(any());
        verify(settlementPlanRepository, never()).flush();
    }

    @Test
    void 수정은_같은_계획의_기본정보와_비용_전체를_교체한다() {
        SettlementPlan plan = storedPlan();
        var oldCosts = List.copyOf(plan.getCostItems());
        var createdAt = plan.getCreatedAt();
        when(settlementPlanRepository.findById(42L)).thenReturn(Optional.of(plan));
        doAnswer(invocation -> {
            // 새 항목 추가 전 기존 항목 삭제를 먼저 요청
            assertThat(plan.getCostItems()).isEmpty();
            assertThat(oldCosts).allSatisfy(costItem -> assertThat(costItem.getSettlementPlan()).isNull());
            return null;
        }).when(settlementPlanRepository).flush();
        var request = request(
                List.of(cost(CostType.AIRFARE, 7_000, CurrencyCode.JPY),
                        cost(CostType.MOVING, 8_000, CurrencyCode.KRW)),
                List.of(cost(CostType.TRANSPORTATION, 9_000, CurrencyCode.JPY)));
        request.setPlannedStayMonths(24);
        request.setPreparedFunds(new CurrencyAmountsRequest(2_000_000L, 200_000L));
        request.setEmergencyReserve(new CurrencyAmountsRequest(200_000L, 20_000L));
        request.setMonthlyLivingCostInputMethod(MonthlyLivingCostInputMethod.DIRECT);

        var response = settlementPlanService.update(42L, request);

        assertThat(plan.getId()).isEqualTo(42L);
        assertThat(plan.getMoveInDate()).isEqualTo(request.getMoveInDate());
        assertThat(plan.getPlannedStayMonths()).isEqualTo(24);
        assertThat(plan.getPreparedFundsKrw()).isEqualTo(2_000_000L);
        assertThat(plan.getPreparedFundsJpy()).isEqualTo(200_000L);
        assertThat(plan.getEmergencyReserveKrw()).isEqualTo(200_000L);
        assertThat(plan.getEmergencyReserveJpy()).isEqualTo(20_000L);
        assertThat(plan.getMonthlyLivingCostInputMethod()).isEqualTo(MonthlyLivingCostInputMethod.DIRECT);
        assertThat(plan.getCreatedAt()).isEqualTo(createdAt);
        assertThat(plan.getCostItems()).extracting(SettlementPlanCostItem::getCostType)
                .containsExactly(CostType.AIRFARE, CostType.MOVING, CostType.TRANSPORTATION);
        assertThat(plan.getCostItems()).allSatisfy(costItem -> assertThat(costItem.getSettlementPlan()).isSameAs(plan));
        assertThat(response.getPlanId()).isEqualTo(42L);
        assertThat(response.getAdditionalInitialCostTotals().getKrw()).isEqualTo(8_000L);
        assertThat(response.getAdditionalInitialCostTotals().getJpy()).isEqualTo(7_000L);
        assertThat(response.getMonthlyLivingCostTotals().getKrw()).isZero();
        assertThat(response.getMonthlyLivingCostTotals().getJpy()).isEqualTo(9_000L);
        verify(settlementPlanRepository).flush();
        verify(settlementPlanRepository, never()).save(any());
    }

    @Test
    void 빈_비용_목록으로_수정하면_기존_항목이_모두_제거된다() {
        SettlementPlan plan = storedPlan();
        var previousUpdatedAt = plan.getUpdatedAt();
        when(settlementPlanRepository.findById(42L)).thenReturn(Optional.of(plan));
        var request = request(List.of(), List.of());
        request.setMoveInDate(plan.getMoveInDate());

        var response = settlementPlanService.update(42L, request);

        assertThat(plan.getCostItems()).isEmpty();
        assertThat(plan.getUpdatedAt()).isAfter(previousUpdatedAt);
        assertThat(response.getAdditionalInitialCosts()).isEmpty();
        assertThat(response.getMonthlyLivingCosts()).isEmpty();
        assertThat(response.getAdditionalInitialCostTotals().getKrw()).isZero();
        assertThat(response.getAdditionalInitialCostTotals().getJpy()).isZero();
        assertThat(response.getMonthlyLivingCostTotals().getKrw()).isZero();
        assertThat(response.getMonthlyLivingCostTotals().getJpy()).isZero();
    }

    @Test
    void 기존_비용_삭제에_실패하면_수정_성공으로_처리하지_않는다() {
        SettlementPlan plan = storedPlan();
        when(settlementPlanRepository.findById(42L)).thenReturn(Optional.of(plan));
        var failure = new IllegalStateException("flush failed");
        doThrow(failure).when(settlementPlanRepository).flush();

        assertThatThrownBy(() -> settlementPlanService.update(42L, request(List.of(), List.of())))
                .isSameAs(failure);
        // 예외 전파만 검증하며 실제 DB rollback 검증은 아님
        verify(settlementPlanRepository, never()).save(any());
    }

    private SettlementPlan storedPlan() {
        var plan = new SettlementPlan(
                LocalDate.now(KOREA_ZONE_ID).plusDays(30), 12,
                1_000_000L, 100_000L, 100_000L, 10_000L, MonthlyLivingCostInputMethod.DEFAULT);
        plan.addCostItem(new SettlementPlanCostItem(CostCategory.INITIAL, CostType.AIRFARE, 1_000, CurrencyCode.KRW));
        plan.addCostItem(new SettlementPlanCostItem(CostCategory.MONTHLY, CostType.FOOD, 2_000, CurrencyCode.JPY));
        ReflectionTestUtils.setField(plan, "id", 42L);
        ReflectionTestUtils.setField(plan, "createdAt", LocalDateTime.of(2026, 1, 1, 0, 0));
        ReflectionTestUtils.setField(plan, "updatedAt", LocalDateTime.of(2026, 1, 1, 0, 0));
        return plan;
    }

    private void saveWithId(long id) {
        when(settlementPlanRepository.save(any(SettlementPlan.class))).thenAnswer(invocation -> {
            SettlementPlan plan = invocation.getArgument(0);
            ReflectionTestUtils.setField(plan, "id", id);
            return plan;
        });
    }

    private void assertErrorCode(SettlementPlanCreateRequest request, ErrorCode errorCode) {
        assertThatThrownBy(() -> settlementPlanService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(errorCode);

        SettlementPlan plan = storedPlan();
        var oldCosts = List.copyOf(plan.getCostItems());
        var previousUpdatedAt = plan.getUpdatedAt();
        when(settlementPlanRepository.findById(42L)).thenReturn(Optional.of(plan));
        var previousResponse = settlementPlanService.get(42L);

        assertThatThrownBy(() -> settlementPlanService.update(42L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(errorCode);
        assertThat(settlementPlanService.get(42L)).usingRecursiveComparison().isEqualTo(previousResponse);
        assertThat(plan.getCostItems()).containsExactlyElementsOf(oldCosts);
        assertThat(plan.getUpdatedAt()).isEqualTo(previousUpdatedAt);
        verify(settlementPlanRepository, never()).flush();
        verify(settlementPlanRepository, never()).save(any());
    }

    private SettlementPlanCreateRequest request(
            List<SettlementPlanCostItemRequest> initialCosts,
            List<SettlementPlanCostItemRequest> monthlyCosts) {
        return new SettlementPlanCreateRequest(
                LocalDate.now(KOREA_ZONE_ID).plusDays(1),
                12,
                new CurrencyAmountsRequest(1_000_000L, 100_000L),
                new CurrencyAmountsRequest(100_000L, 10_000L),
                initialCosts,
                monthlyCosts,
                MonthlyLivingCostInputMethod.DEFAULT);
    }

    private SettlementPlanCostItemRequest cost(CostType type, long amount, CurrencyCode currency) {
        return new SettlementPlanCostItemRequest(type, amount, currency);
    }
}
