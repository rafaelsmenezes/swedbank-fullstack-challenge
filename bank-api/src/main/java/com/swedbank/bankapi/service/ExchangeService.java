package com.swedbank.bankapi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class ExchangeService {

    private static final Map<String, BigDecimal> RATES = Map.of(
            "EUR", BigDecimal.valueOf(1.0),
            "USD", BigDecimal.valueOf(1.08),
            "SEK", BigDecimal.valueOf(11.45),
            "GBP", BigDecimal.valueOf(0.86),
            "VND", BigDecimal.valueOf(27000.0));

    private static final int SCALE = 10;

    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (fromCurrency == null || toCurrency == null) {
            throw new IllegalArgumentException("Currency codes cannot be null");
        }

        BigDecimal fromRate = getRate(fromCurrency);
        BigDecimal toRate = getRate(toCurrency);

        return amount.multiply(toRate.divide(fromRate, SCALE, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getRate(String currency) {
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        BigDecimal rate = RATES.get(currency.toUpperCase());
        if (rate == null) {
            throw new IllegalArgumentException("Unsupported currency: " + currency);
        }
        return rate;
    }
}