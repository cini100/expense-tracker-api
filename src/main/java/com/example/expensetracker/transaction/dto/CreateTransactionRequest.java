package com.example.expensetracker.transaction.dto;

import com.example.expensetracker.common.model.EntryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(
        @NotNull(message = "Category id is required")
        Long categoryId,

        @NotNull(message = "Type is required")
        EntryType type,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Amount supports up to 10 integer digits and 2 decimals")
        BigDecimal amount,

        @Size(max = 500, message = "Description must be 500 characters or fewer")
        String description,

        @NotNull(message = "Transaction date is required")
        @PastOrPresent(message = "Transaction date cannot be in the future")
        LocalDate transactionDate
) {
}
