package com.tajisali.settlement.controller;

import com.tajisali.common.response.ApiResponse;
import com.tajisali.settlement.dto.SettlementPlanCreateRequest;
import com.tajisali.settlement.dto.SettlementPlanCreateResponse;
import com.tajisali.settlement.service.SettlementPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
