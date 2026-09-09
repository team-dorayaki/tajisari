package com.tajisali.property.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "property_cost_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyCostItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cost_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "raw_name", nullable = false)
    private String rawName;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "amount")
    private Long amount;

    @Lob
    @Column(name = "raw_value")
    private String rawValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "obligation_status", nullable = false, length = 20)
    private ObligationStatus obligationStatus;

    @Column(name = "is_included_in_calculation", nullable = false)
    private boolean includedInCalculation;

    @Enumerated(EnumType.STRING)
    @Column(name = "timing", nullable = false, length = 20)
    private CostTiming timing;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isCalculated() {
        return obligationStatus == ObligationStatus.REQUIRED
                || (obligationStatus == ObligationStatus.OPTIONAL && includedInCalculation);
    }
}
