package com.tajisali.settlement.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.settlement.domain.*;
import com.tajisali.settlement.dto.CurrencyAmountsRequest;
import com.tajisali.settlement.dto.SettlementPlanCostItemRequest;
import com.tajisali.settlement.dto.SettlementPlanCreateRequest;
import com.tajisali.settlement.repository.SettlementPlanRepository;
import com.tajisali.user.domain.User;
import com.tajisali.user.repository.UserRepository;
import com.tajisali.user.service.AnonymousUserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SettlementPlanService.class, AnonymousUserService.class})
class CurrentSettlementPlanServiceTest {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final String USER_A_KEY = "00000000-0000-0000-0000-000000000601";
    private static final String USER_B_KEY = "00000000-0000-0000-0000-000000000602";

    @Autowired
    private SettlementPlanService settlementPlanService;

    @Autowired
    private SettlementPlanRepository settlementPlanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 각_사용자는_자신의_정착_계획만_조회한다() {
        Fixture userA = savePlan(USER_A_KEY, 1_000_000L, 1_000L);
        Fixture userB = savePlan(USER_B_KEY, 2_000_000L, 2_000L);
        entityManager.clear();

        var responseA = settlementPlanService.getCurrent(USER_A_KEY);
        var responseB = settlementPlanService.getCurrent(USER_B_KEY);

        assertThat(responseA.getPlanId()).isEqualTo(userA.planId());
        assertThat(responseA.getPreparedFunds().getKrw()).isEqualTo(1_000_000L);
        assertThat(responseB.getPlanId()).isEqualTo(userB.planId());
        assertThat(responseB.getPreparedFunds().getKrw()).isEqualTo(2_000_000L);
    }

    @Test
    void 사용자_A의_수정은_사용자_B의_계획을_변경하지_않는다() {
        Fixture userA = savePlan(USER_A_KEY, 1_000_000L, 1_000L);
        Fixture userB = savePlan(USER_B_KEY, 2_000_000L, 2_000L);
        entityManager.clear();

        var response = settlementPlanService.updateCurrent(USER_A_KEY, updateRequest());
        entityManager.flush();
        entityManager.clear();

        SettlementPlan updatedA = settlementPlanRepository.findByUserId(userA.userId()).orElseThrow();
        SettlementPlan unchangedB = settlementPlanRepository.findByUserId(userB.userId()).orElseThrow();
        assertThat(response.getPlanId()).isEqualTo(userA.planId());
        assertThat(updatedA.getPreparedFundsKrw()).isEqualTo(3_000_000L);
        assertThat(updatedA.getCostItems()).singleElement()
                .extracting(SettlementPlanCostItem::getAmount)
                .isEqualTo(3_000L);
        assertThat(unchangedB.getPreparedFundsKrw()).isEqualTo(2_000_000L);
        assertThat(unchangedB.getCostItems()).singleElement()
                .extracting(SettlementPlanCostItem::getAmount)
                .isEqualTo(2_000L);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "not-a-uuid",
            "00000000-0000-0000-0000-000000000699"
    })
    void Cookie가_없거나_malformed_stale이면_조회하지_않고_User를_생성하지_않는다(String userKey) {
        long userCount = userRepository.count();
        long planCount = settlementPlanRepository.count();

        assertNotFound(() -> settlementPlanService.getCurrent(userKey));

        assertThat(userRepository.count()).isEqualTo(userCount);
        assertThat(settlementPlanRepository.count()).isEqualTo(planCount);
    }

    @Test
    void User는_있지만_계획이_없으면_404이고_데이터를_추가하지_않는다() {
        userRepository.saveAndFlush(new User(USER_A_KEY));
        long userCount = userRepository.count();
        long planCount = settlementPlanRepository.count();

        assertNotFound(() -> settlementPlanService.getCurrent(USER_A_KEY));

        assertThat(userRepository.count()).isEqualTo(userCount);
        assertThat(settlementPlanRepository.count()).isEqualTo(planCount);
    }

    private Fixture savePlan(String userKey, long preparedFundsKrw, long costAmount) {
        User user = userRepository.save(new User(userKey));
        SettlementPlan plan = new SettlementPlan(
                user,
                LocalDate.now(KOREA_ZONE_ID).plusDays(30),
                12,
                preparedFundsKrw,
                100_000L,
                100_000L,
                10_000L,
                MonthlyLivingCostInputMethod.DEFAULT);
        plan.addCostItem(new SettlementPlanCostItem(
                CostCategory.INITIAL,
                CostType.AIRFARE,
                costAmount,
                CurrencyCode.KRW));
        SettlementPlan savedPlan = settlementPlanRepository.saveAndFlush(plan);
        return new Fixture(user.getId(), savedPlan.getId());
    }

    private SettlementPlanCreateRequest updateRequest() {
        return new SettlementPlanCreateRequest(
                LocalDate.now(KOREA_ZONE_ID).plusDays(60),
                18,
                new CurrencyAmountsRequest(3_000_000L, 200_000L),
                new CurrencyAmountsRequest(300_000L, 20_000L),
                List.of(new SettlementPlanCostItemRequest(
                        CostType.AIRFARE,
                        3_000L,
                        CurrencyCode.KRW)),
                List.of(),
                MonthlyLivingCostInputMethod.DIRECT);
    }

    private void assertNotFound(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_PLAN_NOT_FOUND);
    }

    private record Fixture(Long userId, Long planId) {
    }
}
