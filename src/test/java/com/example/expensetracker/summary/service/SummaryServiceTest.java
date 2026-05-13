package com.example.expensetracker.summary.service;

import com.example.expensetracker.auth.service.CurrentUserService;
import com.example.expensetracker.category.entity.Category;
import com.example.expensetracker.common.model.EntryType;
import com.example.expensetracker.summary.dto.MonthlySummaryResponse;
import com.example.expensetracker.transaction.entity.TransactionRecord;
import com.example.expensetracker.transaction.repository.TransactionRepository;
import com.example.expensetracker.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Test
    void monthlySummaryCalculatesTotalsAndExpenseBreakdown() {
        SummaryService summaryService = new SummaryService(transactionRepository, currentUserService);
        User user = user(1L, "user@example.com");
        Category salary = category(10L, user, "Salary", EntryType.INCOME);
        Category food = category(11L, user, "Food", EntryType.EXPENSE);
        Category rent = category(12L, user, "Rent", EntryType.EXPENSE);
        List<TransactionRecord> transactions = List.of(
                transaction(user, salary, EntryType.INCOME, "1000.00"),
                transaction(user, food, EntryType.EXPENSE, "100.00"),
                transaction(user, rent, EntryType.EXPENSE, "300.00")
        );

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(transactionRepository.findByUserIdAndTransactionDateBetween(
                1L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)))
                .thenReturn(transactions);

        MonthlySummaryResponse response = summaryService.getMonthlySummary(2026, 4);

        assertThat(response.incomeTotal()).isEqualByComparingTo("1000.00");
        assertThat(response.expenseTotal()).isEqualByComparingTo("400.00");
        assertThat(response.net()).isEqualByComparingTo("600.00");
        assertThat(response.transactionCount()).isEqualTo(3);
        assertThat(response.breakdown()).hasSize(2);
        assertThat(response.breakdown().getFirst().categoryName()).isEqualTo("Rent");
        assertThat(response.breakdown().getFirst().percentage()).isEqualByComparingTo("75.00");
    }

    private User user(Long id, String email) {
        User user = new User(email, "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Category category(Long id, User user, String name, EntryType type) {
        Category category = new Category(user, name, type);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private TransactionRecord transaction(User user, Category category, EntryType type, String amount) {
        return new TransactionRecord(
                user,
                category,
                type,
                new BigDecimal(amount),
                null,
                LocalDate.of(2026, 4, 10)
        );
    }
}
