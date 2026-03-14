package com.example.expensetracker.category.dto;

import com.example.expensetracker.common.model.EntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 60, message = "Category name must be 60 characters or fewer")
        String name,

        @NotNull(message = "Category type is required")
        EntryType type
) {
}
