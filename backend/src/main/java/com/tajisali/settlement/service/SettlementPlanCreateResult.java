package com.tajisali.settlement.service;

import com.tajisali.settlement.dto.SettlementPlanCreateResponse;

public record SettlementPlanCreateResult(
        SettlementPlanCreateResponse response,
        String userKey
) {
}
