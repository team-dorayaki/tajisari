package com.tajisali.property.controller;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.common.response.ApiResponse;
import com.tajisali.property.dto.PropertyAnalysisResponse;
import com.tajisali.property.dto.PropertyConfirmRequest;
import com.tajisali.property.dto.PropertyConfirmResponse;
import com.tajisali.property.dto.PropertyDetailResponse;
import com.tajisali.property.dto.PropertyListResponse;
import com.tajisali.property.dto.PropertyPriorityUpdateRequest;
import com.tajisali.property.dto.PropertyPriorityUpdateResponse;
import com.tajisali.property.service.PropertyCommandService;
import com.tajisali.property.service.PropertyConfirmResult;
import com.tajisali.property.service.PropertyQueryService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private static final String ANONYMOUS_USER_COOKIE = "tajisari_anonymous_user";
    private static final Duration ANONYMOUS_USER_COOKIE_MAX_AGE = Duration.ofDays(365);
    private static final int MAX_IMAGE_COUNT = 3;
    private static final long MAX_IMAGE_SIZE_BYTES = 15L * 1024 * 1024;
    private static final long MAX_IMAGE_REQUEST_SIZE_BYTES = 50L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp",
            "image/bmp"
    );

    private final PropertyQueryService propertyQueryService;
    private final PropertyCommandService propertyCommandService;

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PropertyConfirmResponse>> confirmUrlProperty(
            @Valid @RequestBody PropertyConfirmRequest request,
            @CookieValue(name = ANONYMOUS_USER_COOKIE, required = false) String userKey) {
        validateUrlRequest(request);
        PropertyConfirmResult result = propertyCommandService.confirmUrl(request, userKey);
        return createdResponse(result, userKey);
    }

    @PostMapping(value = "/confirm/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PropertyConfirmResponse>> confirmImageProperty(
            @Valid @RequestPart("request") PropertyConfirmRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @CookieValue(name = ANONYMOUS_USER_COOKIE, required = false) String userKey) {
        validateImageRequest(request);
        validateImages(files);
        PropertyConfirmResult result = propertyCommandService.confirmImages(request, files, userKey);
        return createdResponse(result, userKey);
    }

    private ResponseEntity<ApiResponse<PropertyConfirmResponse>> createdResponse(
            PropertyConfirmResult result,
            String userKey) {
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(HttpStatus.CREATED);

        if (!Objects.equals(userKey, result.userKey())) {
            ResponseCookie cookie = ResponseCookie.from(ANONYMOUS_USER_COOKIE, result.userKey())
                    .httpOnly(true)
                    .path("/")
                    .sameSite("Lax")
                    .maxAge(ANONYMOUS_USER_COOKIE_MAX_AGE)
                    .build();
            responseBuilder.header(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        return responseBuilder.body(ApiResponse.success(result.response()));
    }

    private void validateImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty() || files.size() > MAX_IMAGE_COUNT) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
        }
        long totalSize = 0;
        for (MultipartFile file : files) {
            if (file == null
                    || file.isEmpty()
                    || file.getSize() > MAX_IMAGE_SIZE_BYTES
                    || !ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
                throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
            }
            totalSize += file.getSize();
            if (totalSize > MAX_IMAGE_REQUEST_SIZE_BYTES) {
                throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
            }
        }
    }

    private void validateUrlRequest(PropertyConfirmRequest request) {
        if (request.sourceType() != PropertyAnalysisResponse.SourceType.URL) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
        }
    }

    private void validateImageRequest(PropertyConfirmRequest request) {
        if (request.sourceType() != PropertyAnalysisResponse.SourceType.IMAGE) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
        }
    }

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
    public ResponseEntity<Void> deleteProperty(
            @PathVariable Long propertyId,
            @CookieValue(name = ANONYMOUS_USER_COOKIE, required = false) String userKey) {
        propertyCommandService.deleteProperty(propertyId, userKey);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/priorities")
    @Operation(summary = "매물 우선순위 일괄 저장 및 변경")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "우선순위 저장 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "동일한 매물을 중복 지정함",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자 또는 사용자의 저장 매물을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<PropertyPriorityUpdateResponse>> updatePriorities(
            @RequestBody PropertyPriorityUpdateRequest request,
            @CookieValue(name = ANONYMOUS_USER_COOKIE, required = false) String userKey) {
        return ResponseEntity.ok(ApiResponse.success(
                propertyCommandService.updatePriorities(request, userKey)));
    }
}
