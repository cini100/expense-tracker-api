package com.example.expensetracker.category.controller;

import com.example.expensetracker.auth.filter.JwtAuthenticationFilter;
import com.example.expensetracker.auth.service.CustomUserDetailsService;
import com.example.expensetracker.auth.service.JwtService;
import com.example.expensetracker.category.service.CategoryService;
import com.example.expensetracker.common.error.UnauthorizedResponseWriter;
import com.example.expensetracker.common.web.PageResponse;
import com.example.expensetracker.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, UnauthorizedResponseWriter.class})
class CategorySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void protectedEndpointWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithInvalidTokenReturnsUnauthorized() throws Exception {
        when(jwtService.extractUsername("bad-token")).thenThrow(new RuntimeException("bad token"));

        mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithValidTokenReachesController() throws Exception {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("user@example.com")
                .password("hash")
                .authorities("ROLE_USER")
                .build();
        when(jwtService.extractUsername("good-token")).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtService.validateToken("good-token", userDetails)).thenReturn(true);
        when(categoryService.getAll(any())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, false));

        mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk());
    }

    @Test
    void validTokenDoesNotConvertControllerErrorsToUnauthorized() throws Exception {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("user@example.com")
                .password("hash")
                .authorities("ROLE_USER")
                .build();
        when(jwtService.extractUsername("good-token")).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtService.validateToken("good-token", userDetails)).thenReturn(true);
        when(categoryService.getById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        mockMvc.perform(get("/api/v1/categories/99")
                        .header("Authorization", "Bearer good-token"))
                .andExpect(status().isNotFound());
    }
}
