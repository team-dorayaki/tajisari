package com.tajisali.property.dto;

import java.util.List;

public record PropertyPriorityUpdateResponse(List<Priority> priorities) {

    public record Priority(Long propertyId, int priorityRank) {
    }
}
