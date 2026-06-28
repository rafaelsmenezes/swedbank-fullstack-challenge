package com.swedbank.bankapi.controller;

import com.swedbank.bankapi.dto.AccountDto;
import com.swedbank.bankapi.dto.CreditRequest;
import com.swedbank.bankapi.dto.DebitRequest;
import com.swedbank.bankapi.dto.TransactionDto;
import com.swedbank.bankapi.mapper.AccountMapper;
import com.swedbank.bankapi.mapper.TransactionMapper;
import com.swedbank.bankapi.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;
    private final TransactionMapper transactionMapper;

    @GetMapping
    public ResponseEntity<List<AccountDto>> getAccountsByUserId(@RequestParam("userId") Long userId) {
        List<AccountDto> accounts = accountService.getAccountsByUserId(userId)
                .stream()
                .map(accountMapper::toDto)
                .toList();
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccountById(@PathVariable Long id) {
        return ResponseEntity.ok(accountMapper.toDto(accountService.getAccountById(id)));
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getBalance(id));
    }

    @PostMapping("/{id}/credit")
    public ResponseEntity<TransactionDto> credit(@PathVariable Long id,
                                                 @RequestBody @Valid CreditRequest request) {
        var transaction = accountService.credit(
                id,
                request.getAmount(),
                request.getFromCurrency(),
                request.getDescription()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionMapper.toDto(transaction));
    }

    @PostMapping("/{id}/debit")
    public ResponseEntity<TransactionDto> debit(@PathVariable Long id,
                                                @RequestBody @Valid DebitRequest request) {
        var transaction = accountService.debit(
                id,
                request.getAmount(),
                request.getCurrency(),
                request.getDescription()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionMapper.toDto(transaction));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<Page<TransactionDto>> getTransactions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TransactionDto> transactionPage = accountService.getTransactions(id, pageable)
                .map(transactionMapper::toDto);
        return ResponseEntity.ok(transactionPage);
    }
}