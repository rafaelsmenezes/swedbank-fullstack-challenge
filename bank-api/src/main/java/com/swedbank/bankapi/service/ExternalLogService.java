package com.swedbank.bankapi.service;

import java.math.BigDecimal;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ExternalLogService {

    private final WebClient webClient;
    private final String externalLogUrl;
    private final long timeoutMillis;

    public ExternalLogService(WebClient webClient,
            @Value("${external.log.url}") String externalLogUrl,
            @Value("${external.log.timeout-ms}") long timeoutMillis) {
        this.webClient = webClient;
        this.externalLogUrl = externalLogUrl;
        this.timeoutMillis = timeoutMillis;
    }

    public void log(String accountId, BigDecimal amount) {
        try {
            webClient.get()
                    .uri(externalLogUrl)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofMillis(timeoutMillis))
                    .onErrorResume(e -> {
                        log.warn("External log failed: {}", e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            log.info("External log successful");
        } catch (Exception e) {
            log.warn("External log failed: {}", e.getMessage());
        }
    }
}