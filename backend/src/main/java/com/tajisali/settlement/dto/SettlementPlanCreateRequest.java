package com.tajisali.settlement.dto;

import com.tajisali.settlement.domain.MonthlyLivingCostInputMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SettlementPlanCreateRequest {

    @NotNull
    private LocalDate moveInDate;

    @NotNull
    @Min(1)
    @Max(24)
    private Integer plannedStayMonths;

    @NotNull
    @Valid
    private CurrencyAmountsRequest preparedFunds;

    @NotNull
    @Valid
    private CurrencyAmountsRequest emergencyReserve;

    @NotNull
    @Size(max = 5)
    private List<@NotNull @Valid SettlementPlanCostItemRequest> additionalInitialCosts;

    @NotNull
    @Size(max = 6)
    private List<@NotNull @Valid SettlementPlanCostItemRequest> monthlyLivingCosts;

    @NotNull
    private MonthlyLivingCostInputMethod monthlyLivingCostInputMethod;
}
