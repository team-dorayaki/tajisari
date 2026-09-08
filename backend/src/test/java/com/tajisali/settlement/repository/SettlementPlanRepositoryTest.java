package com.tajisali.settlement.repository;

import com.tajisali.settlement.domain.MonthlyLivingCostInputMethod;
import com.tajisali.settlement.domain.SettlementPlan;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SettlementPlanRepositoryTest {

    @Autowired
    private SettlementPlanRepository settlementPlanRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 준비자금과_월_생활비_입력_방식이_저장된다() {
        SettlementPlan settlementPlan = new SettlementPlan(
                LocalDate.of(2026, 10, 15),
                12,
                8_000_000L,
                100_000L,
                1_000_000L,
                0L,
                MonthlyLivingCostInputMethod.DEFAULT);

        SettlementPlan savedPlan = settlementPlanRepository.saveAndFlush(settlementPlan);
        entityManager.clear();

        SettlementPlan foundPlan = settlementPlanRepository.findById(savedPlan.getId()).orElseThrow();

        assertThat(foundPlan.getPreparedFundsKrw()).isEqualTo(8_000_000L);
        assertThat(foundPlan.getPreparedFundsJpy()).isEqualTo(100_000L);
        assertThat(foundPlan.getMonthlyLivingCostInputMethod()).isEqualTo(MonthlyLivingCostInputMethod.DEFAULT);
    }
}