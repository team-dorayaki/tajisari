package com.tajisali.property.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.property.domain.Property;
import com.tajisali.property.repository.PropertyRepository;
import com.tajisali.settlement.domain.CostCategory;
import com.tajisali.settlement.domain.CostType;
import com.tajisali.settlement.domain.CurrencyCode;
import com.tajisali.settlement.domain.MonthlyLivingCostInputMethod;
import com.tajisali.settlement.domain.SettlementPlan;
import com.tajisali.settlement.domain.SettlementPlanCostItem;
import com.tajisali.settlement.repository.SettlementPlanRepository;
import com.tajisali.user.domain.User;
import com.tajisali.user.service.AnonymousUserService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyQueryServiceTest {

    @Mock PropertyRepository propertyRepository;
    @Mock SettlementPlanRepository settlementPlanRepository;
    @Mock AnonymousUserService anonymousUserService;
    @Mock PropertyCostCalculationService propertyCostCalculationService;
    private PropertyQueryService propertyQueryService;

    private static final String USER_KEY = "00000000-0000-0000-0000-000000000001";
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        propertyQueryService = new PropertyQueryService(
                propertyRepository, settlementPlanRepository, anonymousUserService,
                propertyCostCalculationService);
    }

    @Test
    void 최신순_매물의_화면용_요약정보를_반환한다() {
        Property property = new Property(
                "요코하마 스튜디오", 65_000L, 245_000L, 70_000L,
                1, LocalDateTime.now());
        ReflectionTestUtils.setField(property, "id", 10L);

        User user = user();
        when(anonymousUserService.findExisting(USER_KEY)).thenReturn(Optional.of(user));
        when(propertyRepository.findAllByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(property));
        SettlementPlan plan = new SettlementPlan(
                LocalDate.now().plusMonths(1), 12,
                8_000_000L, 100_000L, 1_000_000L, 0L,
                MonthlyLivingCostInputMethod.DEFAULT);
        plan.addCostItem(new SettlementPlanCostItem(
                CostCategory.INITIAL, CostType.AIRFARE, 62_000L, CurrencyCode.JPY));
        plan.addCostItem(new SettlementPlanCostItem(
                CostCategory.MONTHLY, CostType.FOOD, 115_000L, CurrencyCode.JPY));
        when(settlementPlanRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(plan));
        var response = propertyQueryService.getProperties(USER_KEY);

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
        when(anonymousUserService.findExisting(null)).thenReturn(Optional.empty());

        var response = propertyQueryService.getProperties(null);

        assertThat(response.totalCount()).isZero();
        assertThat(response.properties()).isEmpty();
    }

    @Test
    void 매물_ID로_비용과_자금_시뮬레이션이_포함된_상세를_조회한다() {
        Property property = new Property(
                "요코하마 스튜디오", 65_000L, 245_000L, 70_000L,
                1, LocalDateTime.now());
        ReflectionTestUtils.setField(property, "id", 10L);
        ReflectionTestUtils.setField(property, "deposit", 65_000L);

        SettlementPlan plan = new SettlementPlan(
                LocalDate.now().plusMonths(1), 12,
                8_000_000L, 100_000L, 1_000_000L, 0L,
                MonthlyLivingCostInputMethod.DEFAULT);
        ReflectionTestUtils.setField(plan, "id", 7L);
        plan.addCostItem(new SettlementPlanCostItem(
                CostCategory.INITIAL, CostType.AIRFARE, 62_000L, CurrencyCode.JPY));
        plan.addCostItem(new SettlementPlanCostItem(
                CostCategory.MONTHLY, CostType.FOOD, 115_000L, CurrencyCode.JPY));

        User user = user();
        when(anonymousUserService.findExisting(USER_KEY)).thenReturn(Optional.of(user));
        when(propertyRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(property));
        when(settlementPlanRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(plan));
        when(propertyCostCalculationService.calculate(property)).thenReturn(
                new PropertyCostCalculationResult(
                        245_000L, 70_000L, 65_000L, 180_000L,
                        false, false, false));

        var response = propertyQueryService.getPropertyDetail(10L, USER_KEY);

        assertThat(response.propertyId()).isEqualTo(10L);
        assertThat(response.settlementPlanId()).isEqualTo(7L);
        assertThat(response.costAnalysis().refundableAmount()).isEqualTo(65_000L);
        assertThat(response.costAnalysis().nonRefundableAmount()).isEqualTo(180_000L);
        assertThat(response.costAnalysis().hasUnknownInitialCosts()).isFalse();
        assertThat(response.costAnalysis().hasUnknownMonthlyCosts()).isFalse();
        assertThat(response.costAnalysis().hasUnclassifiedCosts()).isFalse();
        assertThat(response.simulation().exchangeRate().jpy()).isEqualTo(100);
        assertThat(response.simulation().exchangeRate().krw()).isEqualTo(860);
        assertThat(response.simulation().livingMonths()).isEqualByComparingTo("3.2");
    }

    @Test
    void 다른_사용자의_매물은_상세_조회할_수_없다() {
        User user = user();
        when(anonymousUserService.findExisting(USER_KEY)).thenReturn(Optional.of(user));
        when(propertyRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propertyQueryService.getPropertyDetail(10L, USER_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PROPERTY_NOT_FOUND);
    }

    private User user() {
        User user = new User(USER_KEY);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }
}
