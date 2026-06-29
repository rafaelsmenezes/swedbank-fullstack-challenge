package com.swedbank.bankapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swedbank.bankapi.dto.CreditRequest;
import com.swedbank.bankapi.dto.DebitRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    @Transactional
    void shouldReturnAllAccountsForUser() throws Exception {
        mockMvc.perform(get("/api/v1/accounts?userId=1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[?(@.currency=='EUR')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.currency=='USD')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.currency=='SEK')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.currency=='GBP')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.currency=='VND')]", hasSize(1)));
    }

    @Test
    @Order(2)
    @Transactional
    void shouldReturnAccountById() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.currency", is("EUR")));
    }

    @Test
    @Order(3)
    @Transactional
    void shouldReturn404ForNonExistentAccount() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("Account not found")));
    }

    @Test
    @Order(4)
    @Transactional
    void shouldCreditAccountSuccessfully() throws Exception {
        CreditRequest request = new CreditRequest();
        request.setAmount(new java.math.BigDecimal("100"));
        request.setFromCurrency("USD");
        request.setDescription("Test");

        ObjectMapper mapper = new ObjectMapper();
        mockMvc.perform(post("/api/v1/accounts/1/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type", is("CREDIT")))
                .andExpect(jsonPath("$.balanceAfter", greaterThan(1500.0)));
    }

    @Test
    @Order(5)
    @Transactional
    void shouldDebitAccountSuccessfully() throws Exception {
        DebitRequest request = new DebitRequest();
        request.setAmount(new java.math.BigDecimal("50"));
        request.setCurrency("EUR");
        request.setDescription("Test");

        ObjectMapper mapper = new ObjectMapper();
        mockMvc.perform(post("/api/v1/accounts/1/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type", is("DEBIT")));
    }

    @Test
    @Order(6)
    @Transactional
    void shouldReturn422ForInsufficientFunds() throws Exception {
        DebitRequest request = new DebitRequest();
        request.setAmount(new java.math.BigDecimal("999999"));
        request.setCurrency("EUR");
        request.setDescription("Test");

        ObjectMapper mapper = new ObjectMapper();
        mockMvc.perform(post("/api/v1/accounts/1/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("Insufficient funds")));
    }

    @Test
    @Order(7)
    @Transactional
    void shouldReturn422ForCurrencyMismatchOnDebit() throws Exception {
        DebitRequest request = new DebitRequest();
        request.setAmount(new java.math.BigDecimal("50"));
        request.setCurrency("USD");
        request.setDescription("Test");

        ObjectMapper mapper = new ObjectMapper();
        mockMvc.perform(post("/api/v1/accounts/1/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", containsString("Currency mismatch")));
    }

    @Test
    @Order(8)
    @Transactional
    void shouldReturnPagedTransactions() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1/transactions?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(20)))
                .andExpect(jsonPath("$.last", is(false)));
    }

    @Test
    @Order(9)
    @Transactional
    void shouldConvertCurrencyViaExchangeEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/exchange?from=USD&to=EUR&amount=100"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.convertedAmount", closeTo(92.59, 0.1)));
    }
}
