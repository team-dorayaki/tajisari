package com.tajisali.settlement.domain;

public enum CostType {

    // 06 추가 초기비용
    AIRFARE, // 항공권
    MOVING, // 이사·수하물
    FURNITURE_APPLIANCE, // 가구·가전
    VISA_ADMINISTRATION, // 비자·행정절차

    // 07 월 생활비
    FOOD, // 식비
    TRANSPORTATION, // 교통비
    UTILITIES, // 공과금
    COMMUNICATION, // 통신비
    INSURANCE_TAX, // 보험·세금

    // 두 카테고리에서 사용하는 기타 비용
    OTHER
}
