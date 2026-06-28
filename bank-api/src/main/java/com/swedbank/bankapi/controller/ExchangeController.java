package com.swedbank.bankapi.controller;

import com.swedbank.bankapi.dto.ExchangeResponse;
import com.swedbank.bankapi.service.ExchangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/exchange")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;

    @GetMapping
    public ResponseEntity<ExchangeResponse> convert(
            @RequestParam("from") String fromCurrency,
            @RequestParam("to") String toCurrency,
            @RequestParam("amount") BigDecimal amount) {

        BigDecimal convertedAmount = exchangeService.convert(amount, fromCurrency, toCurrency);
        BigDecimal rate = exchangeService.getRate(toCurrency)
                .divide(exchangeService.getRate(fromCurrency), 10, BigDecimal.ROUND_HALF_UP);

        ExchangeResponse response = ExchangeResponse.builder()
                .fromCurrency(fromCurrency.toUpperCase())
                .toCurrency(toCurrency.toUpperCase())
                .originalAmount(amount)
                .convertedAmount(convertedAmount)
                .rate(rate)
                .build();

        return ResponseEntity.ok(response);
    }
}