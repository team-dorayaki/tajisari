package com.tajisali.settlement.dto;

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
public class CurrencyAmountsRequest {

    @NotNull
    @PositiveOrZero
    private Long krw;

    @NotNull
    @PositiveOrZero
    private Long jpy;
}
