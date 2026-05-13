package com.example.expensetracker.category.service;

import com.example.expensetracker.auth.service.CurrentUserService;
import com.example.expensetracker.category.dto.CategoryResponse;
import com.example.expensetracker.category.dto.CreateCategoryRequest;
import com.example.expensetracker.category.dto.UpdateCategoryRequest;
import com.example.expensetracker.category.entity.Category;
import com.example.expensetracker.category.repository.CategoryRepository;
import com.example.expensetracker.common.web.PageResponse;
import com.example.expensetracker.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;

    public CategoryService(CategoryRepository categoryRepository, CurrentUserService currentUserService) {
        this.categoryRepository = categoryRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        User user = currentUserService.getCurrentUser();
        String name = request.name().trim();

        if (categoryRepository.existsByUserIdAndNameAndType(user.getId(), name, request.type())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists");
        }

        Category category = categoryRepository.save(new Category(user, name, request.type()));
        return toResponse(category);
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getAll(Pageable pageable) {
        User user = currentUserService.getCurrentUser();
        Page<CategoryResponse> page = categoryRepository.findByUserId(user.getId(), pageable)
                .map(this::toResponse);
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        User user = currentUserService.getCurrentUser();
        return toResponse(findOwnedCategory(id, user.getId()));
    }

    @Transactional
    public CategoryResponse update(Long id, UpdateCategoryRequest request) {
        User user = currentUserService.getCurrentUser();
        Category category = findOwnedCategory(id, user.getId());
        String name = request.name().trim();

        categoryRepository.findByUserIdAndNameAndType(user.getId(), name, request.type())
                .filter(existing -> !existing.getId().equals(category.getId()))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists");
                });

        category.setName(name);
        category.setType(request.type());
        return toResponse(category);
    }

    @Transactional
    public void delete(Long id) {
        User user = currentUserService.getCurrentUser();
        Category category = findOwnedCategory(id, user.getId());
        categoryRepository.delete(category);
    }

    private Category findOwnedCategory(Long id, Long userId) {
        return categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    private PageResponse<CategoryResponse> toPageResponse(Page<CategoryResponse> page) {
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
