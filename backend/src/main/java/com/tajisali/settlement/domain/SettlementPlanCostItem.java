package com.tajisali.settlement.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "settlement_plan_cost_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_cost_items_plan_category_type",
        columnNames = {"settlement_plan_id", "cost_category", "cost_type"}
    ),
    check = {
        @CheckConstraint(
            name = "chk_cost_items_category",
            constraint = "cost_category IN ('INITIAL', 'MONTHLY')"
        ),
        @CheckConstraint(name = "chk_cost_items_amount", constraint = "amount >= 0"),
        @CheckConstraint(name = "chk_cost_items_currency", constraint = "currency IN ('KRW', 'JPY')")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementPlanCostItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_plan_cost_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "settlement_plan_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_cost_items_settlement_plan")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SettlementPlan settlementPlan;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "cost_category", nullable = false, length = 16)
    private CostCategory costCategory;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "cost_type", nullable = false, length = 32)
    private CostType costType;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private CurrencyCode currency;

    public SettlementPlanCostItem(
            CostCategory costCategory, CostType costType, long amount, CurrencyCode currency) {
        this.costCategory = costCategory;
        this.costType = costType;
        this.amount = amount;
        this.currency = currency;
    }

    void setSettlementPlan(SettlementPlan settlementPlan) {
        this.settlementPlan = settlementPlan;
    }
}
