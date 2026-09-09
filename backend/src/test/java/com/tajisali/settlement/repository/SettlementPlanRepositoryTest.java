package com.tajisali.settlement.repository;

import com.tajisali.settlement.domain.MonthlyLivingCostInputMethod;
import com.tajisali.settlement.domain.SettlementPlan;
import com.tajisali.user.domain.User;
import com.tajisali.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SettlementPlanRepositoryTest {

    @Autowired
    private SettlementPlanRepository settlementPlanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 준비자금과_월_생활비_입력_방식이_저장된다() {
        User user = userRepository.save(new User("00000000-0000-0000-0000-000000000200"));
        SettlementPlan settlementPlan = createPlan(user);

        SettlementPlan savedPlan = settlementPlanRepository.saveAndFlush(settlementPlan);
        entityManager.clear();

        SettlementPlan foundPlan = settlementPlanRepository.findById(savedPlan.getId()).orElseThrow();

        assertThat(foundPlan.getPreparedFundsKrw()).isEqualTo(8_000_000L);
        assertThat(foundPlan.getPreparedFundsJpy()).isEqualTo(100_000L);
        assertThat(foundPlan.getMonthlyLivingCostInputMethod()).isEqualTo(MonthlyLivingCostInputMethod.DEFAULT);
        assertThat(foundPlan.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void 사용자와_연결된_정착_계획을_저장하고_조회할_수_있다() {
        User user = userRepository.save(new User("00000000-0000-0000-0000-000000000201"));
        SettlementPlan savedPlan = settlementPlanRepository.saveAndFlush(createPlan(user));
        entityManager.clear();

        SettlementPlan foundPlan = settlementPlanRepository.findById(savedPlan.getId()).orElseThrow();

        assertThat(foundPlan.getUser().getId()).isEqualTo(user.getId());
        assertThat(foundPlan.getUser().getUserKey())
                .isEqualTo("00000000-0000-0000-0000-000000000201");
    }

    @Test
    void 같은_사용자에게_정착_계획을_두_개_연결할_수_없다() {
        User user = userRepository.save(new User("00000000-0000-0000-0000-000000000202"));
        settlementPlanRepository.saveAndFlush(createPlan(user));

        assertThatThrownBy(() -> settlementPlanRepository.saveAndFlush(createPlan(user)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 정착_계획이_참조하는_사용자를_삭제할_수_없다() {
        User user = userRepository.save(new User("00000000-0000-0000-0000-000000000203"));
        settlementPlanRepository.saveAndFlush(createPlan(user));
        Long userId = user.getId();
        entityManager.clear();

        userRepository.deleteById(userId);

        assertThatThrownBy(userRepository::flush)
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private SettlementPlan createPlan(User user) {
        return new SettlementPlan(
                user,
                LocalDate.of(2026, 10, 15),
                12,
                8_000_000L,
                100_000L,
                1_000_000L,
                0L,
                MonthlyLivingCostInputMethod.DEFAULT);
    }
}
