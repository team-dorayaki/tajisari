package com.tajisali.property.controller;

import com.tajisali.property.service.PropertyImageQueryService;
import com.tajisali.property.service.PropertyImageStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/property-images")
@RequiredArgsConstructor
public class PropertyImageController {

    private static final String ANONYMOUS_USER_COOKIE = "tajisari_anonymous_user";

    private final PropertyImageQueryService propertyImageQueryService;

    @GetMapping("/{imageId}")
    @Operation(summary = "저장 매물 이미지 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "이미지 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "이미지가 없거나 소유권이 없음")
    })
    public ResponseEntity<Resource> getImage(
            @PathVariable Long imageId,
            @CookieValue(name = ANONYMOUS_USER_COOKIE, required = false) String userKey) {
        PropertyImageStorageService.StoredImageFile image =
                propertyImageQueryService.getImage(imageId, userKey);
        return ResponseEntity.ok()
                .contentType(image.contentType())
                .body(image.resource());
    }
}
