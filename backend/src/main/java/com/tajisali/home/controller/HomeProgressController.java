package com.tajisali.home.controller;

import com.tajisali.common.response.ApiResponse;
import com.tajisali.home.domain.HomeProgressStep;
import com.tajisali.home.dto.HomeProgressResponse;
import com.tajisali.home.service.HomeProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/home-progress")
@RequiredArgsConstructor
public class HomeProgressController {

    private static final String ANONYMOUS_USER_COOKIE = "tajisari_anonymous_user";

    private final HomeProgressService homeProgressService;

    @GetMapping
    public ResponseEntity<ApiResponse<HomeProgressResponse>> getHomeProgress(
            @CookieValue(name = ANONYMOUS_USER_COOKIE, required = false) String userKey) {
        HomeProgressStep currentStep = homeProgressService.getCurrentStep(userKey);
        return ResponseEntity.ok(ApiResponse.success(new HomeProgressResponse(currentStep)));
    }
}
