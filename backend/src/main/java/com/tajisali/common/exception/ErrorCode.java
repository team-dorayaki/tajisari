package com.tajisali.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    COMMON_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식을 확인해 주세요."),
    COMMON_INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),

    SETTLEMENT_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "정착 계획을 찾을 수 없습니다."),
    SETTLEMENT_PLAN_RESERVE_EXCEEDS_FUNDS(HttpStatus.BAD_REQUEST, "비상예비비는 같은 통화의 준비자금을 초과할 수 없습니다."),
    SETTLEMENT_PLAN_INVALID_COST_TYPE(HttpStatus.BAD_REQUEST, "해당 비용 구분에서 사용할 수 없는 비용 종류입니다."),
    SETTLEMENT_PLAN_DUPLICATE_COST_TYPE(HttpStatus.BAD_REQUEST, "같은 비용 구분에 동일한 비용 종류를 중복 입력할 수 없습니다."),
    SETTLEMENT_PLAN_COST_TOTAL_OVERFLOW(HttpStatus.BAD_REQUEST, "비용 합계가 지원하는 금액 범위를 초과했습니다."),

    EXCHANGE_RATE_AMOUNT_OVERFLOW(HttpStatus.BAD_REQUEST, "환산 결과가 지원하는 금액 범위를 초과했습니다."),

    PROPERTY_ANALYSIS_FAILED(HttpStatus.BAD_GATEWAY, "매물 분석에 실패했습니다. 잠시 후 다시 시도해 주세요."),
    PROPERTY_ANALYSIS_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "매물 분석 응답 시간이 초과됐습니다. 다시 시도해 주세요."),

    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus httpStatus;
    private final String message;
}
