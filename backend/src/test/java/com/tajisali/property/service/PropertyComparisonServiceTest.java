package com.tajisali.property.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.property.domain.Property;
import com.tajisali.property.dto.PropertyComparisonRequest;
import com.tajisali.property.repository.PropertyRepository;
import com.tajisali.settlement.domain.MonthlyLivingCostInputMethod;
import com.tajisali.settlement.domain.SettlementPlan;
import com.tajisali.settlement.repository.SettlementPlanRepository;
import com.tajisali.user.domain.User;
import com.tajisali.user.service.AnonymousUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyComparisonServiceTest {

    private static final String USER_KEY = "00000000-0000-0000-0000-000000000001";
    private static final Long USER_ID = 1L;

    @Mock
    private AnonymousUserService anonymousUserService;
    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private SettlementPlanRepository settlementPlanRepository;
    @Mock
    private PropertyCostCalculationService propertyCostCalculationService;
    @Mock
    private PropertyFundSimulationService propertyFundSimulationService;

    private PropertyComparisonService propertyComparisonService;

    @BeforeEach
    void setUp() {
        propertyComparisonService = new PropertyComparisonService(
                anonymousUserService,
                propertyRepository,
                settlementPlanRepository,
                propertyCostCalculationService,
                propertyFundSimulationService);
    }

    @Test
    void 요청한_순서대로_매물을_비교하고_비교_완료_이력을_남긴다() {
        User user = user();
        SettlementPlan plan = plan();
        Property first = property(11L, "신주쿠 원룸 A", 300_000L, 80_000L, 1);
        Property second = property(22L, "요코하마 스튜디오", 250_000L, 70_000L, null);
        PropertyImage firstImage = new PropertyImage(
                first, "properties/11/room.jpg", 0, "room.jpg", LocalDateTime.now());
        ReflectionTestUtils.setField(firstImage, "id", 15L);
        ReflectionTestUtils.setField(first, "images", List.of(firstImage));

        when(anonymousUserService.findExisting(USER_KEY)).thenReturn(Optional.of(user));
        when(settlementPlanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(plan));
        when(propertyRepository.findAllByIdInAndUserId(List.of(11L, 22L), USER_ID))
                .thenReturn(List.of(second, first));
        when(propertyCostCalculationService.calculate(first))
                .thenReturn(costCalculation(80_000L, 220_000L));
        when(propertyCostCalculationService.calculate(second))
                .thenReturn(costCalculation(70_000L, 180_000L));
        when(propertyFundSimulationService.calculate(300_000L, 80_000L, plan))
                .thenReturn(simulation(320_000L, 80_000L, new BigDecimal("4.2"), false));
        when(propertyFundSimulationService.calculate(250_000L, 70_000L, plan))
                .thenReturn(simulation(270_000L, 70_000L, new BigDecimal("5.1"), false));

        var response = propertyComparisonService.compare(
                new PropertyComparisonRequest(List.of(11L, 22L)), USER_KEY);

        assertThat(response.settlementPlanId()).isEqualTo(7L);
        assertThat(response.exchangeRate().jpy()).isEqualTo(100);
        assertThat(response.exchangeRate().krw()).isEqualTo(860);
        assertThat(response.properties())
                .extracting(item -> item.propertyId())
                .containsExactly(11L, 22L);
        assertThat(response.properties().getFirst().thumbnailUrl())
                .isEqualTo("/api/property-images/15");
        assertThat(response.properties().getFirst().costs())
                .extracting(
                        item -> item.confirmedInitialCost(),
                        item -> item.initialSettlementCost(),
                        item -> item.plannedStayHousingCost(),
                        item -> item.refundableAmount(),
                        item -> item.nonRefundableAmount())
                .containsExactly(300_000L, 320_000L, 960_000L, 80_000L, 220_000L);
        assertThat(response.properties().get(1).highlights())
                .extracting(
                        item -> item.lowestInitialSettlementCost(),
                        item -> item.lowestMonthlyHousingCost(),
                        item -> item.longestLivingMonths())
                .containsExactly(true, true, true);
        assertThat((boolean) ReflectionTestUtils.getField(user, "hasComparedProperties")).isTrue();
    }

    @Test
    void 미확인_비용이_있는_매물은_우위_표시에서_제외한다() {
        User user = user();
        SettlementPlan plan = plan();
        Property unknownCostProperty = property(11L, "신주쿠 원룸 A", 200_000L, 60_000L, null);
        ReflectionTestUtils.setField(unknownCostProperty, "managementFee", null);
        Property confirmedProperty = property(22L, "요코하마 스튜디오", 250_000L, 70_000L, null);

        when(anonymousUserService.findExisting(USER_KEY)).thenReturn(Optional.of(user));
        when(settlementPlanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(plan));
        when(propertyRepository.findAllByIdInAndUserId(List.of(11L, 22L), USER_ID))
                .thenReturn(List.of(unknownCostProperty, confirmedProperty));
        when(propertyCostCalculationService.calculate(unknownCostProperty))
                .thenReturn(costCalculation(0L, 120_000L));
        when(propertyCostCalculationService.calculate(confirmedProperty))
                .thenReturn(costCalculation(70_000L, 180_000L));
        when(propertyFundSimulationService.calculate(200_000L, 60_000L, plan))
                .thenReturn(simulation(220_000L, 60_000L, new BigDecimal("8.0"), false));
        when(propertyFundSimulationService.calculate(250_000L, 70_000L, plan))
                .thenReturn(simulation(270_000L, 70_000L, new BigDecimal("5.1"), false));

        var response = propertyComparisonService.compare(
                new PropertyComparisonRequest(List.of(11L, 22L)), USER_KEY);

        assertThat(response.properties().getFirst().costs().unknownCostItemCount()).isEqualTo(1);
        assertThat(response.properties().getFirst().highlights())
                .extracting(
                        item -> item.lowestInitialSettlementCost(),
                        item -> item.lowestMonthlyHousingCost(),
                        item -> item.longestLivingMonths())
                .containsExactly(false, false, false);
        assertThat(response.properties().get(1).highlights())
                .extracting(
                        item -> item.lowestInitialSettlementCost(),
                        item -> item.lowestMonthlyHousingCost(),
                        item -> item.longestLivingMonths())
                .containsExactly(true, true, true);
    }

    @Test
    void 비교_대상이_한개면_오류를_반환하고_조회하지_않는다() {
        assertThatThrownBy(() -> propertyComparisonService.compare(
                new PropertyComparisonRequest(List.of(11L)), USER_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PROPERTY_COMPARISON_INVALID_SIZE);

        verifyNoInteractions(
                anonymousUserService,
                propertyRepository,
                settlementPlanRepository,
                propertyCostCalculationService,
                propertyFundSimulationService);
    }

    @Test
    void 같은_매물을_중복해서_비교할_수_없다() {
        assertThatThrownBy(() -> propertyComparisonService.compare(
                new PropertyComparisonRequest(List.of(11L, 11L)), USER_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PROPERTY_COMPARISON_DUPLICATE_PROPERTY);
    }

    @Test
    void 다른_사용자의_매물이_포함되면_오류를_반환한다() {
        User user = user();
        SettlementPlan plan = plan();
        Property ownedProperty = property(11L, "신주쿠 원룸 A", 300_000L, 80_000L, null);

        when(anonymousUserService.findExisting(USER_KEY)).thenReturn(Optional.of(user));
        when(settlementPlanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(plan));
        when(propertyRepository.findAllByIdInAndUserId(List.of(11L, 22L), USER_ID))
                .thenReturn(List.of(ownedProperty));

        assertThatThrownBy(() -> propertyComparisonService.compare(
                new PropertyComparisonRequest(List.of(11L, 22L)), USER_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PROPERTY_NOT_FOUND);
        assertThat((boolean) ReflectionTestUtils.getField(user, "hasComparedProperties")).isFalse();
    }

    private User user() {
        User user = new User(USER_KEY);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private SettlementPlan plan() {
        SettlementPlan plan = new SettlementPlan(
                LocalDate.now().plusMonths(1), 12,
                8_600_000L, 0L, 0L, 0L,
                MonthlyLivingCostInputMethod.DIRECT);
        ReflectionTestUtils.setField(plan, "id", 7L);
        return plan;
    }

    private Property property(
            Long id,
            String name,
            Long initialCost,
            Long monthlyCost,
            Integer priorityRank) {
        Property property = new Property(
                name, monthlyCost, initialCost, monthlyCost, priorityRank, LocalDateTime.now());
        ReflectionTestUtils.setField(property, "id", id);
        ReflectionTestUtils.setField(property, "deposit", 0L);
        ReflectionTestUtils.setField(property, "keyMoney", 0L);
        ReflectionTestUtils.setField(property, "managementFee", 0L);
        return property;
    }

    private PropertyCostCalculationResult costCalculation(
            Long refundableAmount,
            Long nonRefundableAmount) {
        return new PropertyCostCalculationResult(
                0L, 0L, refundableAmount, nonRefundableAmount,
                false, false, false);
    }

    private PropertyFundSimulationResult simulation(
            long initialCost,
            long monthlyHousingCost,
            BigDecimal livingMonths,
            boolean unlimited) {
        return new PropertyFundSimulationResult(
                1_000_000L,
                initialCost,
                true,
                1_000_000L - initialCost,
                monthlyHousingCost,
                100_000L,
                monthlyHousingCost + 100_000L,
                List.of(new PropertyFundSimulationResult.MonthlyBalance(1, 800_000L)),
                livingMonths,
                unlimited,
                12,
                1_200_000L,
                0L,
                200_000L,
                1_720_000L);
    }
}
