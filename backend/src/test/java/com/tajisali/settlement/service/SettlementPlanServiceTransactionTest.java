package com.tajisali.settlement.service;

import com.tajisali.settlement.domain.MonthlyLivingCostInputMethod;
import com.tajisali.settlement.dto.CurrencyAmountsRequest;
import com.tajisali.settlement.dto.SettlementPlanCreateRequest;
import com.tajisali.settlement.repository.SettlementPlanRepository;
import com.tajisali.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class SettlementPlanServiceTransactionTest {

    @Autowired
    private SettlementPlanService settlementPlanService;

    @Autowired
    private SettlementPlanRepository settlementPlanRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 정착_계획_저장이_실패하면_새_사용자도_함께_롤백된다() {
        long userCount = userRepository.count();
        long planCount = settlementPlanRepository.count();
        SettlementPlanCreateRequest request = new SettlementPlanCreateRequest(
                LocalDate.now().plusDays(30),
                25,
                new CurrencyAmountsRequest(1_000_000L, 100_000L),
                new CurrencyAmountsRequest(100_000L, 10_000L),
                List.of(),
                List.of(),
                MonthlyLivingCostInputMethod.DEFAULT);

        assertThatThrownBy(() -> settlementPlanService.create(request, null))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(userRepository.count()).isEqualTo(userCount);
        assertThat(settlementPlanRepository.count()).isEqualTo(planCount);
    }
}
