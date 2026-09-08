package com.tajisali.settlement.controller;

import com.tajisali.common.response.ApiResponse;
import com.tajisali.settlement.dto.SettlementPlanCreateRequest;
import com.tajisali.settlement.dto.SettlementPlanCreateResponse;
import com.tajisali.settlement.dto.SettlementPlanResponse;
import com.tajisali.settlement.service.SettlementPlanService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settlement-plans")
@RequiredArgsConstructor
public class SettlementPlanController {

    private final SettlementPlanService settlementPlanService;

    // 정착 계획 저장
    @PostMapping
    public ResponseEntity<ApiResponse<SettlementPlanCreateResponse>> create(
            @Valid @RequestBody SettlementPlanCreateRequest request) {
        SettlementPlanCreateResponse response = settlementPlanService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // 정착 계획 조회
    @GetMapping("/{planId}")
    public ResponseEntity<ApiResponse<SettlementPlanResponse>> get(
            @PathVariable("planId") @Positive Long planId) {
        SettlementPlanResponse response = settlementPlanService.get(planId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 정착 계획 수정
    @PutMapping("/{planId}")
    public ResponseEntity<ApiResponse<SettlementPlanResponse>> update(
            @PathVariable("planId") @Positive Long planId,
            @Valid @RequestBody SettlementPlanCreateRequest request) {
        SettlementPlanResponse response = settlementPlanService.update(planId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
