package com.example.expensetracker.summary.dto;

import java.math.BigDecimal;
import java.util.List;

public record MonthlySummaryResponse(
        int year,
        int month,
        BigDecimal incomeTotal,
        BigDecimal expenseTotal,
        BigDecimal net,
        long transactionCount,
        List<CategoryTotalItem> breakdown
) {
}
