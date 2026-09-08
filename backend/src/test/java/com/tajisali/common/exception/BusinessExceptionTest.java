package com.tajisali.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    @Test
    void 비즈니스_예외는_오류_코드와_원인을_보존한다() {
        var cause = new IllegalStateException("internal detail");
        var exception = new BusinessException(ErrorCode.SETTLEMENT_PLAN_DUPLICATE_COST_TYPE, cause);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SETTLEMENT_PLAN_DUPLICATE_COST_TYPE);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.SETTLEMENT_PLAN_DUPLICATE_COST_TYPE.getMessage());
        assertThat(exception.getCause()).isSameAs(cause);
    }
}
