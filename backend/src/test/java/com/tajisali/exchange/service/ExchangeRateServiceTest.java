package com.tajisali.exchange.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeRateServiceTest {

    private final ExchangeRateService exchangeRateService = new ExchangeRateService();

    @Test
    void 원화를_엔화로_환산한다() {
        assertThat(exchangeRateService.convertKrwToJpy(860)).isEqualTo(100);
    }

    @Test
    void 원화를_엔화로_환산할_때_반올림한다() {
        assertThat(exchangeRateService.convertKrwToJpy(1_000)).isEqualTo(116);
    }

    @Test
    void 엔화를_원화로_환산할_때_반올림한다() {
        assertThat(exchangeRateService.convertJpyToKrw(1)).isEqualTo(9);
    }

    @Test
    void 영원은_환산해도_영원이다() {
        assertThat(exchangeRateService.convertKrwToJpy(0)).isZero();
        assertThat(exchangeRateService.convertJpyToKrw(0)).isZero();
    }

    @Test
    void 음수_금액은_환산할_수_없다() {
        assertThatThrownBy(() -> exchangeRateService.convertKrwToJpy(-1))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMON_INVALID_INPUT);
    }

    @Test
    void 환산_결과가_long_범위를_넘으면_예외가_발생한다() {
        assertThatThrownBy(() -> exchangeRateService.convertJpyToKrw(Long.MAX_VALUE))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXCHANGE_RATE_AMOUNT_OVERFLOW);
    }
}
