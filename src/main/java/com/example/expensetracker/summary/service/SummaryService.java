package com.example.expensetracker.summary.service;

import com.example.expensetracker.auth.service.CurrentUserService;
import com.example.expensetracker.common.model.EntryType;
import com.example.expensetracker.summary.dto.CategoryTotalItem;
import com.example.expensetracker.summary.dto.MonthlySummaryResponse;
import com.example.expensetracker.transaction.entity.TransactionRecord;
import com.example.expensetracker.transaction.repository.TransactionRepository;
import com.example.expensetracker.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SummaryService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    public SummaryService(TransactionRepository transactionRepository, CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public MonthlySummaryResponse getMonthlySummary(int year, int month) {
        User user = currentUserService.getCurrentUser();
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        List<TransactionRecord> transactions = transactionRepository
                .findByUserIdAndTransactionDateBetween(user.getId(), start, end);

        BigDecimal incomeTotal = sumByType(transactions, EntryType.INCOME);
        BigDecimal expenseTotal = sumByType(transactions, EntryType.EXPENSE);
        BigDecimal net = incomeTotal.subtract(expenseTotal);
        List<CategoryTotalItem> breakdown = expenseBreakdown(transactions, expenseTotal);

        return new MonthlySummaryResponse(
                year,
                month,
                incomeTotal,
                expenseTotal,
                net,
                transactions.size(),
                breakdown
        );
    }

    private BigDecimal sumByType(List<TransactionRecord> transactions, EntryType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<CategoryTotalItem> expenseBreakdown(List<TransactionRecord> transactions, BigDecimal expenseTotal) {
        Map<Long, CategoryBucket> totalsByCategory = new LinkedHashMap<>();

        transactions.stream()
                .filter(transaction -> transaction.getType() == EntryType.EXPENSE)
                .forEach(transaction -> totalsByCategory
                        .computeIfAbsent(
                                transaction.getCategory().getId(),
                                id -> new CategoryBucket(transaction.getCategory().getName())
                        )
                        .add(transaction.getAmount()));

        return totalsByCategory.entrySet()
                .stream()
                .map(entry -> new CategoryTotalItem(
                        entry.getKey(),
                        entry.getValue().categoryName(),
                        entry.getValue().total(),
                        percentage(entry.getValue().total(), expenseTotal)
                ))
                .sorted(Comparator.comparing(CategoryTotalItem::total).reversed())
                .toList();
    }

    private BigDecimal percentage(BigDecimal categoryTotal, BigDecimal expenseTotal) {
        if (expenseTotal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return categoryTotal
                .multiply(ONE_HUNDRED)
                .divide(expenseTotal, 2, RoundingMode.HALF_UP);
    }

    private static final class CategoryBucket {
        private final String categoryName;
        private BigDecimal total = BigDecimal.ZERO;

        private CategoryBucket(String categoryName) {
            this.categoryName = categoryName;
        }

        private void add(BigDecimal amount) {
            total = total.add(amount);
        }

        private String categoryName() {
            return categoryName;
        }

        private BigDecimal total() {
            return total;
        }
    }
}
