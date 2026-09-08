package com.tajisali.property.controller;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.common.response.ApiResponse;
import com.tajisali.property.dto.PropertyAnalysisResponse;
import com.tajisali.property.dto.PropertyAnalysisUrlRequest;
import com.tajisali.property.service.PropertyAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/property-analyses")
@RequiredArgsConstructor
public class PropertyAnalysisController {

    private static final int MAX_IMAGE_COUNT = 3;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp",
            "image/bmp"
    );

    private final PropertyAnalysisService propertyAnalysisService;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PropertyAnalysisResponse>> analyzeImages(
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        validateImages(files);
        return ResponseEntity.ok(ApiResponse.success(propertyAnalysisService.analyzeImages(files)));
    }

    @PostMapping(value = "/url", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PropertyAnalysisResponse>> analyzeUrl(
            @Valid @RequestBody PropertyAnalysisUrlRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(propertyAnalysisService.analyzeUrl(request.url())));
    }

    private void validateImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty() || files.size() > MAX_IMAGE_COUNT) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
        }
        if (files.stream().anyMatch(file -> file == null
                || file.isEmpty()
                || !ALLOWED_IMAGE_TYPES.contains(file.getContentType()))) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
        }
    }
}
