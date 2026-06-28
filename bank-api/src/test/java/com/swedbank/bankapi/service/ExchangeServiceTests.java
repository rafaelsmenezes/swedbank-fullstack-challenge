package com.swedbank.bankapi.service;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ExchangeService Tests")
class ExchangeServiceTests {

    private final ExchangeService exchangeService = new ExchangeService();

    @Nested
    @DisplayName("convert Tests")
    class ConvertTests {

        @Test
        @DisplayName("Should convert amount between different currencies")
        void testConvertValidCurrencies() {
            // Arrange
            BigDecimal amount = BigDecimal.valueOf(100.00);

            // Act - EUR to USD (1 EUR = 1.08 USD)
            BigDecimal result = exchangeService.convert(amount, "EUR", "USD");

            // Assert
            assertThat(result)
                    .isCloseTo(BigDecimal.valueOf(108.00),
                            within(BigDecimal.valueOf(0.01)));
        }

        @Test
        @DisplayName("Should convert same currency without loss")
        void testConvertSameCurrency() {
            // Arrange
            BigDecimal amount = BigDecimal.valueOf(100.00);

            // Act
            BigDecimal result = exchangeService.convert(amount, "EUR", "EUR");

            // Assert
            assertThat(result)
                    .isEqualTo(BigDecimal.valueOf(100.00).setScale(2));
        }

        @Test
        @DisplayName("Should handle decimal amounts correctly")
        void testConvertDecimalAmount() {
            // Arrange
            BigDecimal amount = BigDecimal.valueOf(123.45);

            // Act - EUR to GBP (1 EUR = 0.86 GBP)
            BigDecimal result = exchangeService.convert(amount, "EUR", "GBP");

            // Assert
            assertThat(result)
                    .isCloseTo(BigDecimal.valueOf(106.17),
                            within(BigDecimal.valueOf(0.01)));
        }

        @Test
        @DisplayName("Should handle small amounts")
        void testConvertSmallAmount() {
            // Arrange
            BigDecimal amount = BigDecimal.valueOf(0.50);

            // Act - EUR to USD
            BigDecimal result = exchangeService.convert(amount, "EUR", "USD");

            // Assert
            assertThat(result)
                    .isCloseTo(BigDecimal.valueOf(0.54),
                            within(BigDecimal.valueOf(0.01)));
        }

        @Test
        @DisplayName("Should handle large amounts")
        void testConvertLargeAmount() {
            // Arrange
            BigDecimal amount = BigDecimal.valueOf(1000000.00);

            // Act - EUR to USD
            BigDecimal result = exchangeService.convert(amount, "EUR", "USD");

            // Assert
            assertThat(result)
                    .isCloseTo(BigDecimal.valueOf(1080000.00),
                            within(BigDecimal.valueOf(10.00)));
        }

        @Test
        @DisplayName("Should convert between multiple currency pairs")
        void testConvertMultipleCurrencyPairs() {
            // Arrange
            BigDecimal amount = BigDecimal.valueOf(100.00);

            // Act
            BigDecimal eurToUsd = exchangeService.convert(amount, "EUR", "USD");
            BigDecimal usdToSek = exchangeService.convert(eurToUsd, "USD", "SEK");
            BigDecimal sekToGbp = exchangeService.convert(usdToSek, "SEK", "GBP");

            // Assert
            assertThat(eurToUsd).isGreaterThan(amount);
            assertThat(usdToSek).isGreaterThan(eurToUsd);
            assertThat(sekToGbp).isLessThan(usdToSek);
        }

        @Test
        @DisplayName("Should handle conversion to VND (high rate)")
        void testConvertToVnd() {
            // Arrange
            BigDecimal amount = BigDecimal.valueOf(100.00);

            // Act - EUR to VND (1 EUR = 27000 VND)
            BigDecimal result = exchangeService.convert(amount, "EUR", "VND");

            // Assert
            assertThat(result)
                    .isCloseTo(BigDecimal.valueOf(2700000.00),
                            within(BigDecimal.valueOf(1000.00)));
        }

        @Test
        @DisplayName("Should use correct rounding (HALF_UP)")
        void testConvertRounding() {
            // Arrange
            BigDecimal amount = BigDecimal.valueOf(10.555);

            // Act - EUR to USD
            BigDecimal result = exchangeService.convert(amount, "EUR", "USD");

            // Assert - Should be rounded to 2 decimal places using HALF_UP
            assertThat(result)
                    .isCloseTo(BigDecimal.valueOf(11.40),
                            within(BigDecimal.valueOf(0.01)));
            assertThat(result.scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should handle case-insensitive currency codes")
        void testConvertCaseInsensitive() {
            // Arrange
            BigDecimal amount = BigDecimal.valueOf(100.00);

            // Act
            BigDecimal result1 = exchangeService.convert(amount, "EUR", "USD");
            BigDecimal result2 = exchangeService.convert(amount, "eur", "usd");
            BigDecimal result3 = exchangeService.convert(amount, "Eur", "UsD");

            // Assert
            assertThat(result1)
                    .isEqualTo(result2)
                    .isEqualTo(result3);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when amount is null")
        void testConvertNullAmount() {
            // Act & Assert
            assertThatThrownBy(() -> exchangeService.convert(null, "EUR", "USD"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Amount cannot be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when from currency is null")
        void testConvertNullFromCurrency() {
            // Act & Assert
            assertThatThrownBy(() -> exchangeService.convert(BigDecimal.valueOf(100), null, "USD"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Currency codes cannot be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when to currency is null")
        void testConvertNullToCurrency() {
            // Act & Assert
            assertThatThrownBy(() -> exchangeService.convert(BigDecimal.valueOf(100), "EUR", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Currency codes cannot be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for unsupported from currency")
        void testConvertUnsupportedFromCurrency() {
            // Act & Assert
            assertThatThrownBy(() -> exchangeService.convert(BigDecimal.valueOf(100), "XXX", "USD"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unsupported currency: XXX");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for unsupported to currency")
        void testConvertUnsupportedToCurrency() {
            // Act & Assert
            assertThatThrownBy(() -> exchangeService.convert(BigDecimal.valueOf(100), "EUR", "XYZ"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unsupported currency: XYZ");
        }
    }

    @Nested
    @DisplayName("getRate Tests")
    class GetRateTests {

        @Test
        @DisplayName("Should return correct rate for supported currency")
        void testGetRateSuccess() {
            // Act & Assert
            assertThat(exchangeService.getRate("EUR"))
                    .isEqualTo(BigDecimal.valueOf(1.0));
            assertThat(exchangeService.getRate("USD"))
                    .isEqualTo(BigDecimal.valueOf(1.08));
            assertThat(exchangeService.getRate("GBP"))
                    .isEqualTo(BigDecimal.valueOf(0.86));
            assertThat(exchangeService.getRate("SEK"))
                    .isEqualTo(BigDecimal.valueOf(11.45));
            assertThat(exchangeService.getRate("VND"))
                    .isEqualTo(BigDecimal.valueOf(27000.0));
        }

        @Test
        @DisplayName("Should handle lowercase currency codes")
        void testGetRateLowercase() {
            // Act
            BigDecimal result1 = exchangeService.getRate("eur");
            BigDecimal result2 = exchangeService.getRate("EUR");

            // Assert
            assertThat(result1).isEqualTo(result2);
        }

        @Test
        @DisplayName("Should handle mixed case currency codes")
        void testGetRateMixedCase() {
            // Act
            BigDecimal result = exchangeService.getRate("EuR");

            // Assert
            assertThat(result).isEqualTo(BigDecimal.valueOf(1.0));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when currency is null")
        void testGetRateNull() {
            // Act & Assert
            assertThatThrownBy(() -> exchangeService.getRate(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Currency cannot be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for unsupported currency")
        void testGetRateUnsupported() {
            // Act & Assert
            assertThatThrownBy(() -> exchangeService.getRate("INVALID"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unsupported currency: INVALID");
        }

        @Test
        @DisplayName("Should throw exception for empty string currency")
        void testGetRateEmptyString() {
            // Act & Assert
            assertThatThrownBy(() -> exchangeService.getRate(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unsupported currency: ");
        }
    }

    @Nested
    @DisplayName("Conversion Accuracy Tests")
    class ConversionAccuracyTests {

        @Test
        @DisplayName("Should maintain conversion precision through multiple conversions")
        void testConversionPrecision() {
            // Arrange - Convert EUR -> USD -> EUR (should return close to original)
            BigDecimal original = BigDecimal.valueOf(1000.00);

            // Act
            BigDecimal toUsd = exchangeService.convert(original, "EUR", "USD");
            BigDecimal backToEur = exchangeService.convert(toUsd, "USD", "EUR");

            // Assert - Should be very close to original (within rounding)
            assertThat(backToEur)
                    .isCloseTo(original, within(BigDecimal.valueOf(1.00)));
        }

        @Test
        @DisplayName("Should handle zero amount")
        void testConvertZeroAmount() {
            // Act
            BigDecimal result = exchangeService.convert(BigDecimal.ZERO, "EUR", "USD");

            // Assert
            assertThat(result).isEqualTo(BigDecimal.ZERO.setScale(2));
        }

        @Test
        @DisplayName("Should preserve scale to 2 decimal places in output")
        void testConvertScale() {
            // Act
            BigDecimal result = exchangeService.convert(
                    BigDecimal.valueOf(100.00), "EUR", "USD");

            // Assert
            assertThat(result.scale()).isEqualTo(2);
        }
    }
}
