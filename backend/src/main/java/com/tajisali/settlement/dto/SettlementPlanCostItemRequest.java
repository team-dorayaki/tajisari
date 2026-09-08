package com.tajisali.settlement.dto;

import com.tajisali.settlement.entity.CostType;
import com.tajisali.settlement.entity.CurrencyCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SettlementPlanCostItemRequest {

    @NotNull
    private CostType type;

    @NotNull
    @PositiveOrZero
    private Long amount;

    @NotNull
    private CurrencyCode currency;
}
