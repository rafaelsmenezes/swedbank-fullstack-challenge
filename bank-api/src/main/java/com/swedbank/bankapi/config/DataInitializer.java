package com.swedbank.bankapi.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import com.swedbank.bankapi.domain.Account;
import com.swedbank.bankapi.domain.Currency;
import com.swedbank.bankapi.domain.Transaction;
import com.swedbank.bankapi.domain.TransactionType;
import com.swedbank.bankapi.domain.User;
import com.swedbank.bankapi.repository.AccountRepository;
import com.swedbank.bankapi.repository.TransactionRepository;
import com.swedbank.bankapi.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

        private final UserRepository userRepository;
        private final AccountRepository accountRepository;
        private final TransactionRepository transactionRepository;

        private static final int START_DAYS_AGO = 50;
        private static final int DAYS_STEP = 2;

        @Override
        public void run(String... args) {
                if (userRepository.count() == 0) {
                        User user = User.builder()
                                        .name("Rafael")
                                        .email("rafael@bank.com")
                                        .build();
                        user = userRepository.save(user);

                        createAccountWithTransactions(user, Currency.EUR, BigDecimal.valueOf(1500.00),
                                        generateEurTransactions());
                        createAccountWithTransactions(user, Currency.USD, BigDecimal.valueOf(800.00),
                                        generateUsdTransactions());
                        createAccountWithTransactions(user, Currency.SEK, BigDecimal.valueOf(5000.00),
                                        generateSekTransactions());
                        createAccountWithTransactions(user, Currency.GBP, BigDecimal.valueOf(1200.00),
                                        generateGbpTransactions());
                        createAccountWithTransactions(user, Currency.VND, BigDecimal.valueOf(25000000.00),
                                        generateVndTransactions());
                }
        }

        private void createAccountWithTransactions(User user, Currency currency, BigDecimal finalBalance,
                        List<TransactionData> txDataList) {
                Account account = Account.builder()
                                .user(user)
                                .currency(currency)
                                .balance(BigDecimal.ZERO)
                                .build();
                account = accountRepository.save(account);

                List<Transaction> transactions = new ArrayList<>();
                BigDecimal balance = BigDecimal.ZERO;
                int dayOffset = START_DAYS_AGO;

                for (TransactionData data : txDataList) {
                        LocalDateTime txDate = LocalDateTime.now().minusDays(dayOffset);
                        BigDecimal newBalance;

                        if (data.type == TransactionType.CREDIT) {
                                newBalance = balance.add(data.amount);
                        } else {
                                newBalance = balance.subtract(data.amount);
                                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                                        newBalance = BigDecimal.ZERO;
                                }
                        }

                        Transaction tx = Transaction.builder()
                                        .account(account)
                                        .type(data.type)
                                        .amount(data.amount)
                                        .balanceAfter(newBalance)
                                        .description(data.description)
                                        .createdAt(txDate)
                                        .build();
                        transactions.add(tx);
                        balance = newBalance;
                        dayOffset -= DAYS_STEP;
                        if (dayOffset < 0)
                                dayOffset = 0;
                }

                transactionRepository.saveAll(transactions);
                account.setBalance(finalBalance);
                accountRepository.save(account);
        }

        private List<TransactionData> generateEurTransactions() {
                List<TransactionData> list = new ArrayList<>();
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(800.00), "Salary"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(200.00), "Rent payment"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(300.00), "Freelance payment"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(80.00), "Groceries"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(150.00), "Refund"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(50.00), "Utilities"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(200.00), "Bank transfer in"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(120.00), "Online shopping"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(100.00), "Investment return"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(60.00), "Restaurant"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(400.00), "Salary"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(150.00), "Rent payment"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(250.00), "Bonus"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(90.00), "Transport"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(180.00), "Freelance payment"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(70.00), "Groceries"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(120.00), "Refund"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(40.00), "Insurance"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(300.00), "Bank transfer in"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(100.00), "ATM withdrawal"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(200.00), "Investment return"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(80.00), "Restaurant"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(150.00), "Rent income"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(60.00), "Utilities"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(250.00), "Bonus"));
                return list;
        }

        private List<TransactionData> generateUsdTransactions() {
                List<TransactionData> list = new ArrayList<>();
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(500.00), "Salary"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(100.00), "Rent payment"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(200.00), "Freelance payment"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(50.00), "Groceries"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(80.00), "Refund"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(30.00), "Utilities"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(150.00), "Bank transfer in"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(70.00), "Online shopping"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(60.00), "Investment return"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(40.00), "Restaurant"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(300.00), "Salary"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(120.00), "Rent payment"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(180.00), "Bonus"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(60.00), "Transport"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(100.00), "Freelance payment"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(40.00), "Groceries"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(90.00), "Refund"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(25.00), "Insurance"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(200.00), "Bank transfer in"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(80.00), "ATM withdrawal"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(120.00), "Investment return"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(50.00), "Restaurant"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(100.00), "Rent income"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(35.00), "Utilities"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(150.00), "Bonus"));
                return list;
        }

        private List<TransactionData> generateSekTransactions() {
                List<TransactionData> list = new ArrayList<>();
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(3000.00), "Salary"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(500.00), "Rent payment"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(1200.00), "Freelance payment"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(300.00), "Groceries"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(600.00), "Refund"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(200.00), "Utilities"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(1000.00), "Bank transfer in"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(400.00), "Online shopping"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(500.00), "Investment return"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(250.00), "Restaurant"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(2500.00), "Salary"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(600.00), "Rent payment"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(800.00), "Bonus"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(300.00), "Transport"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(900.00), "Freelance payment"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(200.00), "Groceries"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(450.00), "Refund"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(150.00), "Insurance"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(1500.00), "Bank transfer in"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(500.00), "ATM withdrawal"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(700.00), "Investment return"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(300.00), "Restaurant"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(600.00), "Rent income"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(200.00), "Utilities"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(1000.00), "Bonus"));
                return list;
        }

        private List<TransactionData> generateGbpTransactions() {
                List<TransactionData> list = new ArrayList<>();
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(450.00), "Salary"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(120.00), "Rent payment"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(280.00), "Freelance payment"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(95.00), "Groceries"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(320.00), "Refund"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(110.00), "Utilities"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(210.00), "Bank transfer in"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(85.00), "Online shopping"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(380.00), "Investment return"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(130.00), "Restaurant"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(520.00), "Salary"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(150.00), "Rent payment"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(290.00), "Bonus"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(105.00), "Transport"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(410.00), "Freelance payment"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(75.00), "Groceries"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(260.00), "Refund"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(90.00), "Insurance"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(350.00), "Bank transfer in"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(140.00), "ATM withdrawal"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(480.00), "Investment return"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(115.00), "Restaurant"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(310.00), "Rent income"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(70.00), "Utilities"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(550.00), "Bonus"));
                return list;
        }

        private List<TransactionData> generateVndTransactions() {
                List<TransactionData> list = new ArrayList<>();
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(4200000.00), "Salary"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(850000.00), "Rent payment"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(3100000.00), "Freelance payment"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(620000.00), "Groceries"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(4800000.00), "Refund"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(720000.00), "Utilities"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(2900000.00), "Bank transfer in"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(950000.00), "Online shopping"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(3900000.00), "Investment return"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(1100000.00), "Restaurant"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(4500000.00), "Salary"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(680000.00), "Rent payment"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(2700000.00), "Bonus"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(540000.00), "Transport"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(5100000.00), "Freelance payment"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(780000.00), "Groceries"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(3400000.00), "Refund"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(920000.00), "Insurance"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(3800000.00), "Bank transfer in"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(1250000.00), "ATM withdrawal"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(4600000.00), "Investment return"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(670000.00), "Restaurant"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(3200000.00), "Rent income"));
                list.add(new TransactionData(TransactionType.DEBIT, BigDecimal.valueOf(580000.00), "Utilities"));
                list.add(new TransactionData(TransactionType.CREDIT, BigDecimal.valueOf(4900000.00), "Bonus"));
                return list;
        }

        private static class TransactionData {
                TransactionType type;
                BigDecimal amount;
                String description;

                TransactionData(TransactionType type, BigDecimal amount, String description) {
                        this.type = type;
                        this.amount = amount;
                        this.description = description;
                }
        }
}