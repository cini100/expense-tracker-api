package com.example.expensetracker.category.repository;

import com.example.expensetracker.category.entity.Category;
import com.example.expensetracker.common.model.EntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    Page<Category> findByUserId(Long userId, Pageable pageable);

    boolean existsByUserIdAndNameAndType(Long userId, String name, EntryType type);
}
