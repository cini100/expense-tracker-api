package com.example.expensetracker.transaction.service;

import com.example.expensetracker.auth.service.CurrentUserService;
import com.example.expensetracker.category.entity.Category;
import com.example.expensetracker.category.repository.CategoryRepository;
import com.example.expensetracker.common.model.EntryType;
import com.example.expensetracker.common.web.PageResponse;
import com.example.expensetracker.transaction.dto.CreateTransactionRequest;
import com.example.expensetracker.transaction.dto.TransactionResponse;
import com.example.expensetracker.transaction.dto.UpdateTransactionRequest;
import com.example.expensetracker.transaction.entity.TransactionRecord;
import com.example.expensetracker.transaction.repository.TransactionRepository;
import com.example.expensetracker.user.entity.User;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Test
    void createTransactionWithOwnedCategory() {
        TransactionService transactionService =
                new TransactionService(transactionRepository, categoryRepository, currentUserService);
        User user = user(1L, "user@example.com");
        Category category = category(10L, user, "Food", EntryType.EXPENSE);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(TransactionRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.create(new CreateTransactionRequest(
                10L,
                EntryType.EXPENSE,
                new BigDecimal("12.50"),
                "Lunch",
                LocalDate.of(2026, 4, 10)
        ));

        assertThat(response.categoryId()).isEqualTo(10L);
        assertThat(response.categoryName()).isEqualTo("Food");
        assertThat(response.amount()).isEqualByComparingTo("12.50");
    }

    @Test
    void createWithForeignCategoryReturnsNotFound() {
        TransactionService transactionService =
                new TransactionService(transactionRepository, categoryRepository, currentUserService);
        User user = user(1L, "user@example.com");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.create(new CreateTransactionRequest(
                99L,
                EntryType.EXPENSE,
                new BigDecimal("12.50"),
                "Lunch",
                LocalDate.of(2026, 4, 10)
        )))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createWithCategoryTypeMismatchReturnsBadRequest() {
        TransactionService transactionService =
                new TransactionService(transactionRepository, categoryRepository, currentUserService);
        User user = user(1L, "user@example.com");
        Category category = category(10L, user, "Salary", EntryType.INCOME);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> transactionService.create(new CreateTransactionRequest(
                10L,
                EntryType.EXPENSE,
                new BigDecimal("12.50"),
                "Lunch",
                LocalDate.of(2026, 4, 10)
        )))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void foreignTransactionIsReturnedAsNotFound() {
        TransactionService transactionService =
                new TransactionService(transactionRepository, categoryRepository, currentUserService);
        User user = user(1L, "user@example.com");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(transactionRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listTransactionsReturnsFilteredPage() {
        TransactionService transactionService =
                new TransactionService(transactionRepository, categoryRepository, currentUserService);
        User user = user(1L, "user@example.com");
        Category category = category(10L, user, "Food", EntryType.EXPENSE);
        TransactionRecord transaction = transaction(user, category, EntryType.EXPENSE, "12.50");
        PageRequest pageable = PageRequest.of(0, 20);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(transactionRepository.findAll(anySpecification(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(transaction), pageable, 1));

        PageResponse<TransactionResponse> response = transactionService.getAll(
                EntryType.EXPENSE,
                10L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                pageable
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().categoryName()).isEqualTo("Food");
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void updateOwnedTransaction() {
        TransactionService transactionService =
                new TransactionService(transactionRepository, categoryRepository, currentUserService);
        User user = user(1L, "user@example.com");
        Category oldCategory = category(10L, user, "Food", EntryType.EXPENSE);
        Category newCategory = category(11L, user, "Groceries", EntryType.EXPENSE);
        TransactionRecord transaction = transaction(user, oldCategory, EntryType.EXPENSE, "12.50");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(transactionRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(transaction));
        when(categoryRepository.findByIdAndUserId(11L, 1L)).thenReturn(Optional.of(newCategory));

        TransactionResponse response = transactionService.update(20L, new UpdateTransactionRequest(
                11L,
                EntryType.EXPENSE,
                new BigDecimal("25.00"),
                "Weekly groceries",
                LocalDate.of(2026, 4, 12)
        ));

        assertThat(response.categoryName()).isEqualTo("Groceries");
        assertThat(response.amount()).isEqualByComparingTo("25.00");
        assertThat(transaction.getCategory().getId()).isEqualTo(11L);
    }

    @Test
    void deleteOwnedTransaction() {
        TransactionService transactionService =
                new TransactionService(transactionRepository, categoryRepository, currentUserService);
        User user = user(1L, "user@example.com");
        Category category = category(10L, user, "Food", EntryType.EXPENSE);
        TransactionRecord transaction = transaction(user, category, EntryType.EXPENSE, "12.50");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(transactionRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(transaction));

        transactionService.delete(20L);

        verify(transactionRepository).delete(transaction);
    }

    @SuppressWarnings("unchecked")
    private Specification<TransactionRecord> anySpecification() {
        return any(Specification.class);
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
        TransactionRecord transaction = new TransactionRecord(
                user,
                category,
                type,
                new BigDecimal(amount),
                null,
                LocalDate.of(2026, 4, 10)
        );
        ReflectionTestUtils.setField(transaction, "id", 20L);
        return transaction;
    }
}
