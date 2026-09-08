package com.tajisali.property.controller;

import com.tajisali.common.response.ApiResponse;
import com.tajisali.property.dto.PropertyListResponse;
import com.tajisali.property.dto.PropertyDetailResponse;
import com.tajisali.property.service.PropertyQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/property-analyses")
@RequiredArgsConstructor
public class PropertyAnalysisController {

    private final PropertyQueryService propertyQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PropertyListResponse>> getProperties() {
        return ResponseEntity.ok(ApiResponse.success(
                propertyQueryService.getProperties()));
    }

    @GetMapping("/{analysisId}")
    public ResponseEntity<ApiResponse<PropertyDetailResponse>> getPropertyDetail(
            @PathVariable Long analysisId) {
        return ResponseEntity.ok(ApiResponse.success(
                propertyQueryService.getPropertyDetail(analysisId)));
    }
}
