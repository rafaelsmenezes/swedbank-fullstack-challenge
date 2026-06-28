package com.swedbank.bankapi.service;

import com.swedbank.bankapi.domain.Account;
import com.swedbank.bankapi.domain.Transaction;
import com.swedbank.bankapi.domain.TransactionType;
import com.swedbank.bankapi.exception.CurrencyMismatchException;
import com.swedbank.bankapi.exception.InsufficientFundsException;
import com.swedbank.bankapi.exception.ResourceNotFoundException;
import com.swedbank.bankapi.repository.AccountRepository;
import com.swedbank.bankapi.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ExchangeService exchangeService;
    private final ExternalLogService externalLogService;

    @Transactional(readOnly = true)
    public List<Account> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long accountId) {
        Account account = getAccountById(accountId);
        return account.getBalance();
    }

    @Transactional
    public Transaction credit(Long accountId, BigDecimal amount, String fromCurrency, String description) {
        Account account = getAccountById(accountId);

        BigDecimal convertedAmount = exchangeService.convert(amount, fromCurrency, account.getCurrency().name());

        BigDecimal newBalance = account.getBalance().add(convertedAmount);
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .account(account)
                .type(TransactionType.CREDIT)
                .amount(convertedAmount)
                .balanceAfter(newBalance)
                .description(description)
                .build();
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction debit(Long accountId, BigDecimal amount, String currency, String description) {
        Account account = getAccountById(accountId);

        if (!account.getCurrency().name().equalsIgnoreCase(currency)) {
            throw new CurrencyMismatchException("Currency mismatch: expected " + account.getCurrency().name() + " but got " + currency);
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        externalLogService.log(accountId.toString(), amount);

        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .account(account)
                .type(TransactionType.DEBIT)
                .amount(amount)
                .balanceAfter(newBalance)
                .description(description)
                .build();
        return transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getTransactions(Long accountId, Pageable pageable) {
        return transactionRepository.findByAccountId(accountId, pageable);
    }

    @Transactional(readOnly = true)
    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }
}