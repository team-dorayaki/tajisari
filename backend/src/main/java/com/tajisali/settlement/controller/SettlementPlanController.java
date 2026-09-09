package com.tajisali.settlement.controller;

import com.tajisali.common.response.ApiResponse;
import com.tajisali.settlement.dto.SettlementPlanCreateRequest;
import com.tajisali.settlement.dto.SettlementPlanCreateResponse;
import com.tajisali.settlement.service.SettlementPlanCreateResult;
import com.tajisali.settlement.service.SettlementPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Objects;

@RestController
@RequestMapping("/api/settlement-plans")
@RequiredArgsConstructor
public class SettlementPlanController {

    private static final String ANONYMOUS_USER_COOKIE = "tajisari_anonymous_user";
    private static final Duration ANONYMOUS_USER_COOKIE_MAX_AGE = Duration.ofDays(365);

    private final SettlementPlanService settlementPlanService;

    // 정착 계획 저장
    @PostMapping
    public ResponseEntity<ApiResponse<SettlementPlanCreateResponse>> create(
            @Valid @RequestBody SettlementPlanCreateRequest request,
            @CookieValue(name = ANONYMOUS_USER_COOKIE, required = false) String userKey) {
        SettlementPlanCreateResult result = settlementPlanService.create(request, userKey);
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
}
