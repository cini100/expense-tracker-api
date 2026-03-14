package com.example.expensetracker.category.dto;

import com.example.expensetracker.common.model.EntryType;

import java.time.Instant;

public record CategoryResponse(
        Long id,
        String name,
        EntryType type,
        Instant createdAt,
        Instant updatedAt
) {
}
