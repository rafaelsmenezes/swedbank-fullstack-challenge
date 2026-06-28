package com.swedbank.bankapi.controller;

import com.swedbank.bankapi.dto.TransactionDto;
import com.swedbank.bankapi.mapper.TransactionMapper;
import com.swedbank.bankapi.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final AccountService accountService;
    private final TransactionMapper transactionMapper;

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionMapper.toDto(accountService.getTransactionById(id)));
    }
}