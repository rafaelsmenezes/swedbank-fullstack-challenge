package com.swedbank.bankapi.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.swedbank.bankapi.domain.Account;
import com.swedbank.bankapi.domain.Currency;
import com.swedbank.bankapi.domain.Transaction;
import com.swedbank.bankapi.domain.TransactionType;
import com.swedbank.bankapi.domain.User;
import com.swedbank.bankapi.exception.CurrencyMismatchException;
import com.swedbank.bankapi.exception.InsufficientFundsException;
import com.swedbank.bankapi.exception.ResourceNotFoundException;
import com.swedbank.bankapi.repository.AccountRepository;
import com.swedbank.bankapi.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Tests")
class AccountServiceTests {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ExchangeService exchangeService;

    @Mock
    private ExternalLogService externalLogService;

    @InjectMocks
    private AccountService accountService;

    private User testUser;
    private Account testAccount;
    private Pageable testPageable;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .build();

        testAccount = Account.builder()
                .id(1L)
                .user(testUser)
                .currency(Currency.EUR)
                .balance(BigDecimal.valueOf(1000.00))
                .createdAt(LocalDateTime.now())
                .build();

        testPageable = PageRequest.of(0, 20);
    }

    @Nested
    @DisplayName("getAccountsByUserId Tests")
    class GetAccountsByUserIdTests {

        @Test
        @DisplayName("Should return list of accounts for valid user")
        void testGetAccountsByUserIdSuccess() {
            // Arrange
            Long userId = 1L;
            List<Account> accounts = List.of(testAccount);
            when(accountRepository.findByUserId(userId)).thenReturn(accounts);

            // Act
            List<Account> result = accountService.getAccountsByUserId(userId);

            // Assert
            assertThat(result)
                    .isNotEmpty()
                    .hasSize(1)
                    .contains(testAccount);
            verify(accountRepository).findByUserId(userId);
        }

        @Test
        @DisplayName("Should return empty list when user has no accounts")
        void testGetAccountsByUserIdEmpty() {
            // Arrange
            Long userId = 999L;
            when(accountRepository.findByUserId(userId)).thenReturn(List.of());

            // Act
            List<Account> result = accountService.getAccountsByUserId(userId);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAccountById Tests")
    class GetAccountByIdTests {

        @Test
        @DisplayName("Should return account when it exists")
        void testGetAccountByIdSuccess() {
            // Arrange
            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

            // Act
            Account result = accountService.getAccountById(1L);

            // Assert
            assertThat(result)
                    .isNotNull()
                    .isEqualTo(testAccount);
            verify(accountRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when account not found")
        void testGetAccountByIdNotFound() {
            // Arrange
            when(accountRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> accountService.getAccountById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Account not found");
        }
    }

    @Nested
    @DisplayName("getBalance Tests")
    class GetBalanceTests {

        @Test
        @DisplayName("Should return correct balance for account")
        void testGetBalanceSuccess() {
            // Arrange
            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

            // Act
            BigDecimal balance = accountService.getBalance(1L);

            // Assert
            assertThat(balance)
                    .isEqualTo(BigDecimal.valueOf(1000.00));
        }

        @Test
        @DisplayName("Should throw exception when account not found")
        void testGetBalanceNotFound() {
            // Arrange
            when(accountRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> accountService.getBalance(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("credit Tests")
    class CreditTests {

        @Test
        @DisplayName("Should credit account with same currency conversion")
        void testCreditSameCurrency() {
            // Arrange
            BigDecimal amount = BigDecimal.valueOf(100.00);
            BigDecimal convertedAmount = BigDecimal.valueOf(100.00);

            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
            when(exchangeService.convert(amount, "EUR", "EUR")).thenReturn(convertedAmount);
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction tx = invocation.getArgument(0);
                tx.setId(1L);
                return tx;
            });

            // Act
            Transaction result = accountService.credit(1L, amount, "EUR", "Test credit");

            // Assert
            assertThat(result)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("type", TransactionType.CREDIT)
                    .hasFieldOrPropertyWithValue("amount", convertedAmount)
                    .hasFieldOrPropertyWithValue("description", "Test credit");

            ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(accountCaptor.capture());
            assertThat(accountCaptor.getValue().getBalance())
                    .isEqualTo(BigDecimal.valueOf(1100.00));
        }

        @Test
        @DisplayName("Should credit account with currency conversion")
        void testCreditWithCurrencyConversion() {
            // Arrange
            BigDecimal amount = BigDecimal.valueOf(100.00);
            BigDecimal convertedAmount = BigDecimal.valueOf(108.00);

            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
            when(exchangeService.convert(amount, "USD", "EUR")).thenReturn(convertedAmount);
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction tx = invocation.getArgument(0);
                tx.setId(1L);
                return tx;
            });

            // Act
            Transaction result = accountService.credit(1L, amount, "USD", "Conversion credit");

            // Assert
            assertThat(result.getAmount()).isEqualTo(convertedAmount);
            verify(exchangeService).convert(amount, "USD", "EUR");
        }

        @Test
        @DisplayName("Should throw exception when account not found during credit")
        void testCreditAccountNotFound() {
            // Arrange
            when(accountRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> accountService.credit(999L, BigDecimal.valueOf(100), "EUR", "Test"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should save transaction with correct balance after credit")
        void testCreditTransactionBalance() {
            // Arrange
            BigDecimal amount = BigDecimal.valueOf(250.00);
            BigDecimal convertedAmount = BigDecimal.valueOf(250.00);

            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
            when(exchangeService.convert(amount, "EUR", "EUR")).thenReturn(convertedAmount);
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction tx = invocation.getArgument(0);
                tx.setId(1L);
                return tx;
            });

            // Act
            Transaction result = accountService.credit(1L, amount, "EUR", "Test");

            // Assert
            assertThat(result.getBalanceAfter())
                    .isEqualTo(BigDecimal.valueOf(1250.00));
        }
    }

    @Nested
    @DisplayName("debit Tests")
    class DebitTests {

        @Test
        @DisplayName("Should debit account with matching currency")
        void testDebitSuccess() {
            // Arrange
            BigDecimal amount = BigDecimal.valueOf(100.00);

            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction tx = invocation.getArgument(0);
                tx.setId(1L);
                return tx;
            });

            // Act
            Transaction result = accountService.debit(1L, amount, "EUR", "Test debit");

            // Assert
            assertThat(result)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("type", TransactionType.DEBIT)
                    .hasFieldOrPropertyWithValue("amount", amount);

            verify(externalLogService).log("1", amount);
            ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(accountCaptor.capture());
            assertThat(accountCaptor.getValue().getBalance())
                    .isEqualTo(BigDecimal.valueOf(900.00));
        }

        @Test
        @DisplayName("Should throw CurrencyMismatchException for mismatched currency")
        void testDebitCurrencyMismatch() {
            // Arrange
            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

            // Act & Assert
            assertThatThrownBy(() -> accountService.debit(1L, BigDecimal.valueOf(100), "USD", "Test"))
                    .isInstanceOf(CurrencyMismatchException.class)
                    .hasMessageContaining("Currency mismatch")
                    .hasMessageContaining("EUR")
                    .hasMessageContaining("USD");
        }

        @Test
        @DisplayName("Should throw InsufficientFundsException when balance is insufficient")
        void testDebitInsufficientFunds() {
            // Arrange
            BigDecimal amount = BigDecimal.valueOf(2000.00);
            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

            // Act & Assert
            assertThatThrownBy(() -> accountService.debit(1L, amount, "EUR", "Test"))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessage("Insufficient funds");

            verify(externalLogService, never()).log(anyString(), any());
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should call external log service during debit")
        void testDebitCallsExternalLog() {
            // Arrange
            BigDecimal amount = BigDecimal.valueOf(50.00);

            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction tx = invocation.getArgument(0);
                tx.setId(1L);
                return tx;
            });

            // Act
            accountService.debit(1L, amount, "EUR", "Test");

            // Assert
            verify(externalLogService).log("1", amount);
        }

        @Test
        @DisplayName("Should not save transaction if external log fails")
        void testDebitExternalLogFailureDoesNotPreventDebit() {
            // Arrange
            BigDecimal amount = BigDecimal.valueOf(50.00);

            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
            doThrow(new RuntimeException("External service unavailable"))
                    .when(externalLogService).log("1", amount);

            // Act & Assert - In current implementation, exception would propagate
            // This test documents the current behavior
            assertThatThrownBy(() -> accountService.debit(1L, amount, "EUR", "Test"))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Should save correct balance after debit")
        void testDebitTransactionBalance() {
            // Arrange
            BigDecimal amount = BigDecimal.valueOf(350.00);

            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction tx = invocation.getArgument(0);
                tx.setId(1L);
                return tx;
            });

            // Act
            Transaction result = accountService.debit(1L, amount, "EUR", "Test");

            // Assert
            assertThat(result.getBalanceAfter())
                    .isEqualTo(BigDecimal.valueOf(650.00));
        }
    }

    @Nested
    @DisplayName("getTransactions Tests")
    class GetTransactionsTests {

        @Test
        @DisplayName("Should return paginated transactions for account")
        void testGetTransactionsSuccess() {
            // Arrange
            Transaction tx1 = Transaction.builder()
                    .id(1L)
                    .account(testAccount)
                    .type(TransactionType.CREDIT)
                    .amount(BigDecimal.valueOf(100.00))
                    .balanceAfter(BigDecimal.valueOf(1100.00))
                    .createdAt(LocalDateTime.now())
                    .build();

            Transaction tx2 = Transaction.builder()
                    .id(2L)
                    .account(testAccount)
                    .type(TransactionType.DEBIT)
                    .amount(BigDecimal.valueOf(50.00))
                    .balanceAfter(BigDecimal.valueOf(1050.00))
                    .createdAt(LocalDateTime.now())
                    .build();

            Page<Transaction> page = new PageImpl<>(List.of(tx1, tx2), testPageable, 2);
            when(transactionRepository.findByAccountId(1L, testPageable)).thenReturn(page);

            // Act
            Page<Transaction> result = accountService.getTransactions(1L, testPageable);

            // Assert
            assertThat(result)
                    .isNotNull()
                    .hasSize(2);
            assertThat(result.getContent())
                    .containsExactly(tx1, tx2);
            verify(transactionRepository).findByAccountId(1L, testPageable);
        }

        @Test
        @DisplayName("Should return empty page when no transactions")
        void testGetTransactionsEmpty() {
            // Arrange
            Page<Transaction> emptyPage = new PageImpl<>(List.of(), testPageable, 0);
            when(transactionRepository.findByAccountId(1L, testPageable)).thenReturn(emptyPage);

            // Act
            Page<Transaction> result = accountService.getTransactions(1L, testPageable);

            // Assert
            assertThat(result)
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("getTransactionById Tests")
    class GetTransactionByIdTests {

        @Test
        @DisplayName("Should return transaction when it exists")
        void testGetTransactionByIdSuccess() {
            // Arrange
            Transaction transaction = Transaction.builder()
                    .id(1L)
                    .account(testAccount)
                    .type(TransactionType.CREDIT)
                    .amount(BigDecimal.valueOf(100.00))
                    .balanceAfter(BigDecimal.valueOf(1100.00))
                    .createdAt(LocalDateTime.now())
                    .build();

            when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

            // Act
            Transaction result = accountService.getTransactionById(1L);

            // Assert
            assertThat(result)
                    .isNotNull()
                    .isEqualTo(transaction);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when transaction not found")
        void testGetTransactionByIdNotFound() {
            // Arrange
            when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> accountService.getTransactionById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Transaction not found");
        }
    }
}
