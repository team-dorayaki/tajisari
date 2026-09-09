package com.tajisali.property.dto;

public record PropertyPriorityUpdateRequest(
        Long firstPriorityPropertyId,
        Long secondPriorityPropertyId
) {
}
