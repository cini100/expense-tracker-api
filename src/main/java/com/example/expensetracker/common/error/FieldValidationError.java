package com.example.expensetracker.common.error;

public record FieldValidationError(
        String field,
        String message
) {
}
