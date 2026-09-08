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
import java.time.ZoneId;
import java.util.List;

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
