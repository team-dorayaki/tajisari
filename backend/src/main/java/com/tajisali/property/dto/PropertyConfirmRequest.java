package com.tajisali.property.dto;

import com.tajisali.property.domain.CostTiming;
import com.tajisali.property.domain.ObligationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

public record PropertyConfirmRequest(
        @NotNull PropertyAnalysisResponse.SourceType sourceType,
        @NotBlank String modelVersion,
        @Valid @NotNull PropertyInfo property,
        @Valid @NotNull List<PropertyCostItem> propertyCostItems,
        @NotNull JsonNode rawResult
) {
    @AssertTrue
    public boolean isUrlAnalysis() {
        return sourceType == PropertyAnalysisResponse.SourceType.URL;
    }

    @AssertTrue
    public boolean hasHttpSourceUrl() {
        if (property == null || property.sourceUrl() == null || property.sourceUrl().isBlank()) {
            return false;
        }
        try {
            URI sourceUrl = URI.create(property.sourceUrl());
            return ("http".equalsIgnoreCase(sourceUrl.getScheme())
                    || "https".equalsIgnoreCase(sourceUrl.getScheme()))
                    && sourceUrl.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @AssertTrue
    public boolean hasRawResult() {
        return rawResult != null && !rawResult.isNull();
    }

    public record PropertyInfo(
            @NotNull PropertyAnalysisResponse.SourceSite sourceSite,
            @NotBlank String sourceUrl,
            @NotBlank String propertyName,
            String prefecture,
            String city,
            BigDecimal exclusiveAreaM2,
            String nearestStation,
            Integer walkMinutes,
            Long rent,
            Long managementFee,
            Long deposit,
            Long keyMoney,
            LocalDate availableFrom,
            Integer contractPeriodMonths,
            Long listedInitialCostTotal
    ) {
    }

    public record PropertyCostItem(
            @NotBlank String rawName,
            @NotBlank String displayName,
            @PositiveOrZero Long amount,
            String rawValue,
            @NotNull ObligationStatus obligationStatus,
            boolean includedInCalculation,
            @NotNull CostTiming timing
    ) {
    }
}
