package com.example.expensetracker.transaction.dto;

import com.example.expensetracker.common.model.EntryType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        Long categoryId,
        String categoryName,
        EntryType type,
        BigDecimal amount,
        String description,
        LocalDate transactionDate,
        Instant createdAt,
        Instant updatedAt
) {
}
