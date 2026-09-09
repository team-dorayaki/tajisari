package com.tajisali.settlement.repository;

import com.tajisali.settlement.domain.SettlementPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementPlanRepository extends JpaRepository<SettlementPlan, Long> {

    boolean existsByUserId(Long userId);

    @EntityGraph(attributePaths = "costItems")
    Optional<SettlementPlan> findByUserId(Long userId);

    @EntityGraph(attributePaths = "costItems")
    Optional<SettlementPlan> findTopByOrderByCreatedAtDesc();
}
