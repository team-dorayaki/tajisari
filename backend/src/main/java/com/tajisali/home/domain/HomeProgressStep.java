package com.tajisali.home.domain;

/**
 * 홈 화면에 표시할 사용자 현재 진행 단계.
 *
 * <p>판정은 STEP_1 → COMPLETED 순서로 short-circuit 방식으로 수행한다.
 */
public enum HomeProgressStep {

    /** 정착 계획이 없음. */
    STEP_1,

    /** 정착 계획은 있으나 저장된 매물이 없음. */
    STEP_2,

    /** 저장 매물은 있으나 비교 완료 이력이 없음. */
    STEP_3,

    /** 비교 완료 이력은 있으나 우선순위 매물이 없음. */
    STEP_4,

    /** 모든 단계를 완료함. */
    COMPLETED
}
