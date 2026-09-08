package com.tajisali.property.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "property")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_id")
    private Long id;

    @Column(name = "source_site", length = 50)
    private String sourceSite;

    @Column(name = "source_url", length = 2048)
    private String sourceUrl;

    @Column(name = "property_name")
    private String propertyName;

    @Column(name = "prefecture", length = 50)
    private String prefecture;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "exclusive_area_m2", precision = 8, scale = 2)
    private BigDecimal exclusiveAreaM2;

    @Column(name = "nearest_station", length = 100)
    private String nearestStation;

    @Column(name = "walk_minutes")
    private Integer walkMinutes;

    @Column(name = "rent")
    private Long rent;

    @Column(name = "management_fee")
    private Long managementFee;

    @Column(name = "deposit")
    private Long deposit;

    @Column(name = "key_money")
    private Long keyMoney;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Column(name = "contract_period_months")
    private Integer contractPeriodMonths;

    @Column(name = "listed_initial_cost_total")
    private Long listedInitialCostTotal;

    @Column(name = "confirmed_initial_cost")
    private Long confirmedInitialCost;

    @Column(name = "confirmed_monthly_cost")
    private Long confirmedMonthlyCost;

    @Column(name = "priority_rank", unique = true)
    private Integer priorityRank;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY)
    @OrderBy("imageOrder ASC")
    private List<PropertyImage> images = new ArrayList<>();

    public Property(
            String propertyName,
            Long rent,
            Long confirmedInitialCost,
            Long confirmedMonthlyCost,
            Integer priorityRank,
            LocalDateTime createdAt) {
        this.propertyName = propertyName;
        this.rent = rent;
        this.confirmedInitialCost = confirmedInitialCost;
        this.confirmedMonthlyCost = confirmedMonthlyCost;
        this.priorityRank = priorityRank;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }
}
