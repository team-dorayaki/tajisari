package com.tajisali.settlement.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "settlement_plans",
    check = {
        @CheckConstraint(
            name = "chk_settlement_plans_stay_months",
            constraint = "planned_stay_months BETWEEN 1 AND 24"
        ),
        @CheckConstraint(
            name = "chk_settlement_plans_no_income_months",
            constraint = "no_income_period_months BETWEEN 0 AND planned_stay_months"
        ),
        @CheckConstraint(
            name = "chk_settlement_plans_available_funds",
            constraint = "available_funds_krw >= 0 AND available_funds_jpy >= 0"
        ),
        @CheckConstraint(
            name = "chk_settlement_plans_emergency_reserve",
            constraint = "emergency_reserve_krw BETWEEN 0 AND available_funds_krw "
                    + "AND emergency_reserve_jpy BETWEEN 0 AND available_funds_jpy"
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementPlan {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_plan_id")
    private Long id;

    @Column(name = "move_in_date", nullable = false)
    private LocalDate moveInDate;

    @Column(name = "planned_stay_months", nullable = false)
    private int plannedStayMonths;

    @Column(name = "no_income_period_months", nullable = false)
    private int noIncomePeriodMonths;

    @Column(name = "available_funds_krw", nullable = false)
    private long availableFundsKrw;

    @Column(name = "available_funds_jpy", nullable = false)
    private long availableFundsJpy;

    @Column(name = "emergency_reserve_krw", nullable = false)
    private long emergencyReserveKrw;

    @Column(name = "emergency_reserve_jpy", nullable = false)
    private long emergencyReserveJpy;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "settlementPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SettlementPlanCostItem> costItems = new ArrayList<>();

    public SettlementPlan(
            LocalDate moveInDate,
            int plannedStayMonths,
            int noIncomePeriodMonths,
            long availableFundsKrw,
            long availableFundsJpy,
            long emergencyReserveKrw,
            long emergencyReserveJpy) {
        this.moveInDate = moveInDate;
        this.plannedStayMonths = plannedStayMonths;
        this.noIncomePeriodMonths = noIncomePeriodMonths;
        this.availableFundsKrw = availableFundsKrw;
        this.availableFundsJpy = availableFundsJpy;
        this.emergencyReserveKrw = emergencyReserveKrw;
        this.emergencyReserveJpy = emergencyReserveJpy;
    }

    // 비용 항목 추가
    public void addCostItem(SettlementPlanCostItem costItem) {
        costItems.add(costItem);
        costItem.setSettlementPlan(this);
    }

    // 생성일시 설정
    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now(KOREA_ZONE_ID);
        createdAt = now;
        updatedAt = now;
    }

    // 수정일시 설정
    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now(KOREA_ZONE_ID);
    }
}
