package com.tajisali.home.dto;

import com.tajisali.home.domain.HomeProgressStep;

/**
 * 홈 진행 단계 조회 응답.
 *
 * @param currentStep 현재 진행 단계. STEP_1, STEP_2, STEP_3, STEP_4, COMPLETED 중 하나.
 */
public record HomeProgressResponse(HomeProgressStep currentStep) {
}
