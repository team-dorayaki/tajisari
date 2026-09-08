package com.tajisali.property.dto;

import java.math.BigDecimal;
import java.util.List;

public record PropertyListResponse(long totalCount, List<PropertySummary> properties) {

    public record PropertySummary(
            Long propertyId,
            Long analysisId,
            String propertyName,
            Long rent,
            Long initialCost,
            BigDecimal livingMonths,
            Integer priorityRank,
            String thumbnailUrl) {
    }
}
