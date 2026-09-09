package com.tajisali.settlement.controller;

import com.tajisali.common.response.ApiResponse;
import com.tajisali.settlement.dto.SettlementPlanCreateRequest;
import com.tajisali.settlement.dto.SettlementPlanResponse;
import com.tajisali.settlement.service.SettlementPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/settlement-plan")
@RequiredArgsConstructor
public class CurrentSettlementPlanController {

    private static final String ANONYMOUS_USER_COOKIE = "tajisari_anonymous_user";

    private final SettlementPlanService settlementPlanService;

    @GetMapping
    public ResponseEntity<ApiResponse<SettlementPlanResponse>> get(
            @CookieValue(name = ANONYMOUS_USER_COOKIE, required = false) String userKey) {
        SettlementPlanResponse response = settlementPlanService.getCurrent(userKey);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<SettlementPlanResponse>> update(
            @CookieValue(name = ANONYMOUS_USER_COOKIE, required = false) String userKey,
            @Valid @RequestBody SettlementPlanCreateRequest request) {
        SettlementPlanResponse response = settlementPlanService.updateCurrent(userKey, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
