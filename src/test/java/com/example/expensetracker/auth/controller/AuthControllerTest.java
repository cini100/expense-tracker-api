package com.example.expensetracker.auth.controller;

import com.example.expensetracker.auth.dto.AuthResponse;
import com.example.expensetracker.auth.dto.RegisterRequest;
import com.example.expensetracker.auth.service.AuthService;
import com.example.expensetracker.auth.service.CustomUserDetailsService;
import com.example.expensetracker.auth.service.JwtService;
import com.example.expensetracker.common.error.UnauthorizedResponseWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private UnauthorizedResponseWriter unauthorizedResponseWriter;

    @Test
    void registerReturnsCreated() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse("token", "Bearer", 3600));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("user@example.com", "password123"))))
                .andExpect(status().isCreated());
    }

    @Test
    void malformedRegisterRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("bad-email", "short"))))
                .andExpect(status().isBadRequest());
    }
}
