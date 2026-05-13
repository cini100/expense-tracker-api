package com.example.expensetracker.category.service;

import com.example.expensetracker.auth.service.CurrentUserService;
import com.example.expensetracker.category.dto.CategoryResponse;
import com.example.expensetracker.category.dto.CreateCategoryRequest;
import com.example.expensetracker.category.dto.UpdateCategoryRequest;
import com.example.expensetracker.category.entity.Category;
import com.example.expensetracker.category.repository.CategoryRepository;
import com.example.expensetracker.common.model.EntryType;
import com.example.expensetracker.common.web.PageResponse;
import com.example.expensetracker.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Test
    void createCategoryForCurrentUser() {
        CategoryService categoryService = new CategoryService(categoryRepository, currentUserService);
        User user = user(1L, "user@example.com");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.existsByUserIdAndNameAndType(1L, "Food", EntryType.EXPENSE)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = categoryService.create(new CreateCategoryRequest(" Food ", EntryType.EXPENSE));

        assertThat(response.name()).isEqualTo("Food");
        assertThat(response.type()).isEqualTo(EntryType.EXPENSE);
    }

    @Test
    void duplicateCategoryReturnsConflict() {
        CategoryService categoryService = new CategoryService(categoryRepository, currentUserService);
        User user = user(1L, "user@example.com");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.existsByUserIdAndNameAndType(1L, "Food", EntryType.EXPENSE)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(new CreateCategoryRequest("Food", EntryType.EXPENSE)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void foreignCategoryIsReturnedAsNotFound() {
        CategoryService categoryService = new CategoryService(categoryRepository, currentUserService);
        User user = user(1L, "user@example.com");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listCategoriesReturnsPageResponse() {
        CategoryService categoryService = new CategoryService(categoryRepository, currentUserService);
        User user = user(1L, "user@example.com");
        Category category = category(10L, user, "Food", EntryType.EXPENSE);
        PageRequest pageable = PageRequest.of(0, 20);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.findByUserId(1L, pageable)).thenReturn(new PageImpl<>(List.of(category), pageable, 1));

        PageResponse<CategoryResponse> response = categoryService.getAll(pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().name()).isEqualTo("Food");
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void updateOwnedCategory() {
        CategoryService categoryService = new CategoryService(categoryRepository, currentUserService);
        User user = user(1L, "user@example.com");
        Category category = category(10L, user, "Food", EntryType.EXPENSE);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(category));
        when(categoryRepository.findByUserIdAndNameAndType(1L, "Groceries", EntryType.EXPENSE))
                .thenReturn(Optional.empty());

        CategoryResponse response = categoryService.update(10L, new UpdateCategoryRequest("Groceries", EntryType.EXPENSE));

        assertThat(response.name()).isEqualTo("Groceries");
        assertThat(category.getName()).isEqualTo("Groceries");
    }

    @Test
    void deleteOwnedCategory() {
        CategoryService categoryService = new CategoryService(categoryRepository, currentUserService);
        User user = user(1L, "user@example.com");
        Category category = category(10L, user, "Food", EntryType.EXPENSE);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(category));

        categoryService.delete(10L);

        verify(categoryRepository).delete(category);
    }

    private User user(Long id, String email) {
        User user = new User(email, "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Category category(Long id, User user, String name, EntryType type) {
        Category category = new Category(user, name, type);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }
}
