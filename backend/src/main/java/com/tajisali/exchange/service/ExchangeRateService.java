package com.tajisali.exchange.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ExchangeRateService {

    public static final long KRW_PER_100_JPY = 860L;
    public static final long BASE_JPY = 100L;

    private static final BigDecimal KRW_PER_100_JPY_DECIMAL = BigDecimal.valueOf(KRW_PER_100_JPY);
    private static final BigDecimal BASE_JPY_DECIMAL = BigDecimal.valueOf(BASE_JPY);

    public long convertKrwToJpy(long krw) {
        validateNonNegative(krw);
        BigDecimal convertedAmount = BigDecimal.valueOf(krw)
                .multiply(BASE_JPY_DECIMAL)
                .divide(KRW_PER_100_JPY_DECIMAL, 0, RoundingMode.HALF_UP);
        return toLong(convertedAmount);
    }

    public long convertJpyToKrw(long jpy) {
        validateNonNegative(jpy);
        BigDecimal convertedAmount = BigDecimal.valueOf(jpy)
                .multiply(KRW_PER_100_JPY_DECIMAL)
                .divide(BASE_JPY_DECIMAL, 0, RoundingMode.HALF_UP);
        return toLong(convertedAmount);
    }

    private void validateNonNegative(long amount) {
        if (amount < 0) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
        }
    }

    private long toLong(BigDecimal convertedAmount) {
        try {
            return convertedAmount.longValueExact();
        } catch (ArithmeticException exception) {
            throw new BusinessException(ErrorCode.EXCHANGE_RATE_AMOUNT_OVERFLOW, exception);
        }
    }
}
