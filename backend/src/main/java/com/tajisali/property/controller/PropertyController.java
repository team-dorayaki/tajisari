package com.tajisali.property.controller;

import com.tajisali.common.response.ApiResponse;
import com.tajisali.property.dto.PropertyDetailResponse;
import com.tajisali.property.dto.PropertyListResponse;
import com.tajisali.property.service.PropertyCommandService;
import com.tajisali.property.service.PropertyQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyQueryService propertyQueryService;
    private final PropertyCommandService propertyCommandService;

    @GetMapping
    public ResponseEntity<ApiResponse<PropertyListResponse>> getProperties() {
        return ResponseEntity.ok(ApiResponse.success(
                propertyQueryService.getProperties()));
    }

    @GetMapping("/{propertyId}")
    public ResponseEntity<ApiResponse<PropertyDetailResponse>> getPropertyDetail(
            @PathVariable Long propertyId) {
        return ResponseEntity.ok(ApiResponse.success(
                propertyQueryService.getPropertyDetail(propertyId)));
    }

    @DeleteMapping("/{propertyId}")
    @Operation(summary = "저장 매물 삭제")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "저장 매물 삭제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "저장된 매물을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<Void> deleteProperty(@PathVariable Long propertyId) {
        propertyCommandService.deleteProperty(propertyId);
        return ResponseEntity.noContent().build();
    }
}
