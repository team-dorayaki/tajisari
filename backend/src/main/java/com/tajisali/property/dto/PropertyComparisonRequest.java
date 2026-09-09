package com.tajisali.property.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PropertyComparisonRequest(
        @NotNull
        @Size(min = 2, max = 3)
        List<@NotNull Long> propertyIds) {
}
