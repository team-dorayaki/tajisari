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

import jakarta.persistence.CascadeType;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "settlement_plans",
        check = {
                @CheckConstraint(
                        name = "chk_settlement_plans_stay_months",
                        constraint = "planned_stay_months BETWEEN 1 AND 24"
                ),
                @CheckConstraint(
                        name = "chk_settlement_plans_prepared_funds",
                        constraint = "prepared_funds_krw >= 0 AND prepared_funds_jpy >= 0"
                ),
                @CheckConstraint(
                        name = "chk_settlement_plans_emergency_reserve",
                        constraint = "emergency_reserve_krw BETWEEN 0 AND prepared_funds_krw "
                                + "AND emergency_reserve_jpy BETWEEN 0 AND prepared_funds_jpy"
                ),
                @CheckConstraint(
                        name = "chk_settlement_plans_monthly_cost_input_method",
                        constraint = "monthly_living_cost_input_method IN ('DIRECT', 'DEFAULT')"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementPlan {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_plan_id", comment = "정착 계획 식별자")
    private Long id;

    @Column(name = "move_in_date", nullable = false, comment = "입주 예정일")
    private LocalDate moveInDate;

    @Column(name = "planned_stay_months", nullable = false, comment = "예상 체류기간(개월)")
    private int plannedStayMonths;

    @Column(name = "prepared_funds_krw", nullable = false, comment = "원화 준비자금")
    private long preparedFundsKrw;

    @Column(name = "prepared_funds_jpy", nullable = false, comment = "엔화 준비자금")
    private long preparedFundsJpy;

    @Column(name = "emergency_reserve_krw", nullable = false, comment = "원화 비상예비비")
    private long emergencyReserveKrw;

    @Column(name = "emergency_reserve_jpy", nullable = false, comment = "엔화 비상예비비")
    private long emergencyReserveJpy;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(
            name = "monthly_living_cost_input_method",
            nullable = false,
            length = 16,
            comment = "월 생활비 입력 방식"
    )
    private MonthlyLivingCostInputMethod monthlyLivingCostInputMethod;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)", comment = "생성일시")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)", comment = "수정일시")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "settlementPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SettlementPlanCostItem> costItems = new ArrayList<>();

    public SettlementPlan(
            LocalDate moveInDate,
            int plannedStayMonths,
            long preparedFundsKrw,
            long preparedFundsJpy,
            long emergencyReserveKrw,
            long emergencyReserveJpy,
            MonthlyLivingCostInputMethod monthlyLivingCostInputMethod) {
        this.moveInDate = moveInDate;
        this.plannedStayMonths = plannedStayMonths;
        this.preparedFundsKrw = preparedFundsKrw;
        this.preparedFundsJpy = preparedFundsJpy;
        this.emergencyReserveKrw = emergencyReserveKrw;
        this.emergencyReserveJpy = emergencyReserveJpy;
        this.monthlyLivingCostInputMethod = monthlyLivingCostInputMethod;
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
