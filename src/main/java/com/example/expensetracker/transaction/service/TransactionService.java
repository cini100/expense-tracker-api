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
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;

    public TransactionService(TransactionRepository transactionRepository, CategoryRepository categoryRepository,
                              CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request) {
        User user = currentUserService.getCurrentUser();
        Category category = findOwnedCategory(request.categoryId(), user.getId());
        validateCategoryType(category, request.type());

        TransactionRecord transaction = transactionRepository.save(new TransactionRecord(
                user,
                category,
                request.type(),
                request.amount(),
                request.description(),
                request.transactionDate()
        ));
        return toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getAll(EntryType type, Long categoryId, LocalDate from, LocalDate to,
                                                    Pageable pageable) {
        User user = currentUserService.getCurrentUser();
        Page<TransactionResponse> page = transactionRepository
                .findAll(filteredBy(user.getId(), type, categoryId, from, to), pageable)
                .map(this::toResponse);
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id) {
        User user = currentUserService.getCurrentUser();
        return toResponse(findOwnedTransaction(id, user.getId()));
    }

    @Transactional
    public TransactionResponse update(Long id, UpdateTransactionRequest request) {
        User user = currentUserService.getCurrentUser();
        TransactionRecord transaction = findOwnedTransaction(id, user.getId());
        Category category = findOwnedCategory(request.categoryId(), user.getId());
        validateCategoryType(category, request.type());

        transaction.setCategory(category);
        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setDescription(request.description());
        transaction.setTransactionDate(request.transactionDate());
        return toResponse(transaction);
    }

    @Transactional
    public void delete(Long id) {
        User user = currentUserService.getCurrentUser();
        TransactionRecord transaction = findOwnedTransaction(id, user.getId());
        transactionRepository.delete(transaction);
    }

    private Category findOwnedCategory(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private TransactionRecord findOwnedTransaction(Long id, Long userId) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    private void validateCategoryType(Category category, EntryType type) {
        if (category.getType() != type) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction type must match category type");
        }
    }

    private Specification<TransactionRecord> filteredBy(Long userId, EntryType type, Long categoryId,
                                                        LocalDate from, LocalDate to) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));

            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), to));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private TransactionResponse toResponse(TransactionRecord transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getCategory().getId(),
                transaction.getCategory().getName(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }

    private PageResponse<TransactionResponse> toPageResponse(Page<TransactionResponse> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
