package com.tajisali.property.controller;

import com.tajisali.common.response.ApiResponse;
import com.tajisali.property.dto.PropertyComparisonRequest;
import com.tajisali.property.dto.PropertyComparisonResponse;
import com.tajisali.property.service.PropertyComparisonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/property-comparisons")
@RequiredArgsConstructor
public class PropertyComparisonController {

    private static final String ANONYMOUS_USER_COOKIE = "tajisari_anonymous_user";

    private final PropertyComparisonService propertyComparisonService;

    @PostMapping
    @Operation(summary = "저장 매물 복수 비교")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "매물 비교 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "비교 대상 수 또는 중복 매물 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "정착 계획 또는 저장 매물을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<PropertyComparisonResponse>> compare(
            @Valid @RequestBody PropertyComparisonRequest request,
            @CookieValue(name = ANONYMOUS_USER_COOKIE, required = false) String userKey) {
        return ResponseEntity.ok(ApiResponse.success(
                propertyComparisonService.compare(request, userKey)));
    }
}
