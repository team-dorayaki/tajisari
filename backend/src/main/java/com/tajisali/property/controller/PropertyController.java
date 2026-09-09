package com.tajisali.property.controller;

import com.tajisali.common.response.ApiResponse;
import com.tajisali.property.dto.PropertyDetailResponse;
import com.tajisali.property.dto.PropertyListResponse;
import com.tajisali.property.service.PropertyQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private static final String ANONYMOUS_USER_COOKIE = "tajisari_anonymous_user";

    private final PropertyQueryService propertyQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PropertyListResponse>> getProperties(
            @CookieValue(name = ANONYMOUS_USER_COOKIE, required = false) String userKey) {
        return ResponseEntity.ok(ApiResponse.success(
                propertyQueryService.getProperties(userKey)));
    }

    @GetMapping("/{propertyId}")
    public ResponseEntity<ApiResponse<PropertyDetailResponse>> getPropertyDetail(
            @PathVariable Long propertyId,
            @CookieValue(name = ANONYMOUS_USER_COOKIE, required = false) String userKey) {
        return ResponseEntity.ok(ApiResponse.success(
                propertyQueryService.getPropertyDetail(propertyId, userKey)));
    }
}
