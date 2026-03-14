package com.example.expensetracker.summary.dto;

import java.math.BigDecimal;

public record CategoryTotalItem(
        Long categoryId,
        String categoryName,
        BigDecimal total,
        BigDecimal percentage
) {
}
