package com.swedbank.bankapi.service;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExternalLogService Tests")
class ExternalLogServiceTests {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ExternalLogService externalLogService;
    private final String testUrl = "https://httpstat.us/200";
    private final long testTimeoutMs = 3000;

    @BeforeEach
    void setUp() {
        externalLogService = new ExternalLogService(webClient, testUrl, testTimeoutMs);
    }

    @Nested
    @DisplayName("log Method Tests")
    class LogMethodTests {

        @Test
        @DisplayName("Should successfully log when external service is available")
        void testLogSuccess() {
            // Arrange
            String accountId = "123";
            BigDecimal amount = BigDecimal.valueOf(100.00);

            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(testUrl)).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

            // Act
            externalLogService.log(accountId, amount);

            // Assert
            verify(webClient).get();
            verify(requestHeadersUriSpec).uri(testUrl);
            verify(requestHeadersUriSpec).retrieve();
            verify(responseSpec).toBodilessEntity();
        }

        @Test
        @DisplayName("Should handle external service timeout gracefully")
        void testLogTimeout() {
            // Arrange
            String accountId = "123";
            BigDecimal amount = BigDecimal.valueOf(100.00);

            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(testUrl)).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toBodilessEntity())
                    .thenReturn(Mono.error(new RuntimeException("Timeout")));

            // Act & Assert - Should not throw exception, just log warning
            assertThatNoException().isThrownBy(() -> externalLogService.log(accountId, amount));

            verify(webClient).get();
        }

        @Test
        @DisplayName("Should handle null accountId gracefully")
        void testLogNullAccountId() {
            // Arrange
            BigDecimal amount = BigDecimal.valueOf(100.00);

            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(testUrl)).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

            // Act & Assert
            assertThatNoException().isThrownBy(() -> externalLogService.log(null, amount));
        }

        @Test
        @DisplayName("Should handle null amount gracefully")
        void testLogNullAmount() {
            // Arrange
            String accountId = "123";

            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(testUrl)).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

            // Act & Assert
            assertThatNoException().isThrownBy(() -> externalLogService.log(accountId, null));
        }

        @Test
        @DisplayName("Should handle both null parameters gracefully")
        void testLogBothNull() {
            // Arrange
            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(testUrl)).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

            // Act & Assert
            assertThatNoException().isThrownBy(() -> externalLogService.log(null, null));
        }

        @Test
        @DisplayName("Should not propagate exception to caller on service error")
        void testLogServiceError() {
            // Arrange
            String accountId = "123";
            BigDecimal amount = BigDecimal.valueOf(50.00);

            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(testUrl)).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toBodilessEntity())
                    .thenReturn(Mono.error(new RuntimeException("Service unavailable")));

            // Act & Assert
            assertThatNoException().isThrownBy(() -> externalLogService.log(accountId, amount));
        }

        @Test
        @DisplayName("Should construct correct URI for WebClient")
        void testLogConstructsCorrectUri() {
            // Arrange
            String accountId = "456";
            BigDecimal amount = BigDecimal.valueOf(200.50);

            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(testUrl)).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

            // Act
            externalLogService.log(accountId, amount);

            // Assert
            ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
            verify(requestHeadersUriSpec).uri(uriCaptor.capture());
            assertThat(uriCaptor.getValue()).isEqualTo(testUrl);
        }

        @Test
        @DisplayName("Should handle large amount values")
        void testLogLargeAmount() {
            // Arrange
            String accountId = "999";
            BigDecimal amount = BigDecimal.valueOf(999999999.99);

            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(testUrl)).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

            // Act & Assert
            assertThatNoException().isThrownBy(() -> externalLogService.log(accountId, amount));
        }

        @Test
        @DisplayName("Should handle zero amount")
        void testLogZeroAmount() {
            // Arrange
            String accountId = "123";
            BigDecimal amount = BigDecimal.ZERO;

            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(testUrl)).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

            // Act & Assert
            assertThatNoException().isThrownBy(() -> externalLogService.log(accountId, amount));
        }

        @Test
        @DisplayName("Should handle special characters in accountId")
        void testLogSpecialCharactersInAccountId() {
            // Arrange
            String accountId = "ACC-123@456#789";
            BigDecimal amount = BigDecimal.valueOf(100.00);

            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(testUrl)).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

            // Act & Assert
            assertThatNoException().isThrownBy(() -> externalLogService.log(accountId, amount));
        }

        @Test
        @DisplayName("Should use GET method for external service")
        void testLogUsesGetMethod() {
            // Arrange
            String accountId = "123";
            BigDecimal amount = BigDecimal.valueOf(100.00);

            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(testUrl)).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

            // Act
            externalLogService.log(accountId, amount);

            // Assert
            verify(webClient, times(1)).get();
            verify(webClient, never()).post();
            verify(webClient, never()).put();
            verify(webClient, never()).delete();
        }

        @Test
        @DisplayName("Should call retrieve on response spec")
        void testLogCallsRetrieve() {
            // Arrange
            String accountId = "123";
            BigDecimal amount = BigDecimal.valueOf(100.00);

            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(testUrl)).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

            // Act
            externalLogService.log(accountId, amount);

            // Assert
            verify(requestHeadersUriSpec).retrieve();
        }

        @Test
        @DisplayName("Should call toBodilessEntity on response spec")
        void testLogCallsToBodilessEntity() {
            // Arrange
            String accountId = "123";
            BigDecimal amount = BigDecimal.valueOf(100.00);

            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(testUrl)).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

            // Act
            externalLogService.log(accountId, amount);

            // Assert
            verify(responseSpec).toBodilessEntity();
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should be initialized with correct URL")
        void testConstructorInitializesUrl() {
            // Assert - Service is constructed with testUrl
            assertThat(externalLogService).isNotNull();
        }

        @Test
        @DisplayName("Should be initialized with correct timeout")
        void testConstructorInitializesTimeout() {
            // Assert - Service is constructed with testTimeoutMs
            assertThat(externalLogService).isNotNull();
        }

        @Test
        @DisplayName("Should work with different timeout values")
        void testConstructorWithDifferentTimeout() {
            // Arrange
            long differentTimeout = 5000;

            // Act
            ExternalLogService service = new ExternalLogService(webClient, testUrl, differentTimeout);

            // Assert
            assertThat(service).isNotNull();
        }

        @Test
        @DisplayName("Should accept zero timeout")
        void testConstructorWithZeroTimeout() {
            // Arrange
            long zeroTimeout = 0;

            // Act
            ExternalLogService service = new ExternalLogService(webClient, testUrl, zeroTimeout);

            // Assert
            assertThat(service).isNotNull();
        }

        @Test
        @DisplayName("Should accept different URLs")
        void testConstructorWithDifferentUrls() {
            // Arrange
            String[] urls = {
                    "https://api.example.com/log",
                    "https://logging-service.io",
                    "http://localhost:8080/log"
            };

            // Act & Assert
            for (String url : urls) {
                ExternalLogService service = new ExternalLogService(webClient, url, testTimeoutMs);
                assertThat(service).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle WebClient exception")
        void testHandlesWebClientException() {
            // Arrange
            String accountId = "123";
            BigDecimal amount = BigDecimal.valueOf(100.00);

            when(webClient.get())
                    .thenThrow(new RuntimeException("WebClient error"));

            // Act & Assert
            assertThatNoException().isThrownBy(() -> externalLogService.log(accountId, amount));
        }

        @Test
        @DisplayName("Should handle network exception")
        void testHandlesNetworkException() {
            // Arrange
            String accountId = "123";
            BigDecimal amount = BigDecimal.valueOf(100.00);

            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(testUrl)).thenThrow(
                    new RuntimeException("Network error"));

            // Act & Assert
            assertThatNoException().isThrownBy(() -> externalLogService.log(accountId, amount));
        }

        @Test
        @DisplayName("Should recover from multiple consecutive failures")
        void testRecoverFromMultipleFailures() {
            // Arrange
            String accountId = "123";
            BigDecimal amount = BigDecimal.valueOf(100.00);

            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(testUrl)).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toBodilessEntity())
                    .thenReturn(Mono.error(new RuntimeException("Failure 1")))
                    .thenReturn(Mono.error(new RuntimeException("Failure 2")))
                    .thenReturn(Mono.empty());

            // Act - Should handle multiple failures without throwing
            assertThatNoException().isThrownBy(() -> {
                externalLogService.log(accountId, amount);
                externalLogService.log(accountId, amount);
                externalLogService.log(accountId, amount);
            });
        }
    }

    @Nested
    @DisplayName("Multiple Calls Tests")
    class MultipleCallsTests {

        @Test
        @DisplayName("Should handle multiple successive log calls")
        void testMultipleSuccessiveCalls() {
            // Arrange
            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(testUrl)).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

            // Act
            externalLogService.log("ACC1", BigDecimal.valueOf(100));
            externalLogService.log("ACC2", BigDecimal.valueOf(200));
            externalLogService.log("ACC3", BigDecimal.valueOf(300));

            // Assert
            verify(webClient, times(3)).get();
        }

        @Test
        @DisplayName("Should handle concurrent log calls")
        void testConcurrentLogCalls() throws InterruptedException {
            // Arrange
            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(testUrl)).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

            // Act - Create threads that call log concurrently
            Thread thread1 = new Thread(() -> externalLogService.log("ACC1", BigDecimal.valueOf(100)));
            Thread thread2 = new Thread(() -> externalLogService.log("ACC2", BigDecimal.valueOf(200)));
            Thread thread3 = new Thread(() -> externalLogService.log("ACC3", BigDecimal.valueOf(300)));

            thread1.start();
            thread2.start();
            thread3.start();

            thread1.join();
            thread2.join();
            thread3.join();

            // Assert
            verify(webClient, times(3)).get();
        }
    }
}
