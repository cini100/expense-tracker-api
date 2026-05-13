package com.example.expensetracker.integration;

import com.example.expensetracker.category.repository.CategoryRepository;
import com.example.expensetracker.transaction.repository.TransactionRepository;
import com.example.expensetracker.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ApiFlowIntegrationTest {

    private static final String TEST_JWT_SECRET =
            "ZmFrZS1kZXYtc2VjcmV0LWZvci1sb2NhbC11c2Utb25seS0xMjM0NTY3ODkw";

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine")
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.jwt.secret", () -> TEST_JWT_SECRET);
        registry.add("app.jwt.expiration-minutes", () -> "60");
    }

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void fullApiFlowCreatesTransactionsAndMonthlySummary() throws Exception {
        String token = registerAndGetToken("FLOW@EXAMPLE.COM", "password123");
        long expenseCategoryId = createCategory(token, "Food", "EXPENSE");
        long incomeCategoryId = createCategory(token, "Salary", "INCOME");

        createTransaction(token, expenseCategoryId, "EXPENSE", "12.50", "Lunch", "2024-04-10")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryName").value("Food"));

        createTransaction(token, incomeCategoryId, "INCOME", "1000.00", "April salary", "2024-04-01")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryName").value("Salary"));

        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", bearer(token))
                        .param("type", "EXPENSE")
                        .param("categoryId", String.valueOf(expenseCategoryId))
                        .param("from", "2024-04-01")
                        .param("to", "2024-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].categoryName").value("Food"));

        MvcResult summaryResult = mockMvc.perform(get("/api/v1/summaries/monthly")
                        .header("Authorization", bearer(token))
                        .param("year", "2024")
                        .param("month", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionCount").value(2))
                .andExpect(jsonPath("$.breakdown[0].categoryName").value("Food"))
                .andReturn();

        JsonNode summary = objectMapper.readTree(summaryResult.getResponse().getContentAsString());
        assertThat(summary.get("incomeTotal").decimalValue()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(summary.get("expenseTotal").decimalValue()).isEqualByComparingTo(new BigDecimal("12.50"));
        assertThat(summary.get("net").decimalValue()).isEqualByComparingTo(new BigDecimal("987.50"));
    }

    @Test
    void authFailuresAndValidationReturnExpectedStatuses() throws Exception {
        registerAndGetToken("dupe@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"DUPE@example.com","password":"password123"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"dupe@example.com","password":"wrongpass"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bad-email","password":"short"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpointsRequireValidToken() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));

        mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", bearer("not-a-real-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
    }

    @Test
    void resourcesAreIsolatedPerUser() throws Exception {
        String ownerToken = registerAndGetToken("owner@example.com", "password123");
        String otherToken = registerAndGetToken("other@example.com", "password123");
        long ownerCategoryId = createCategory(ownerToken, "Food", "EXPENSE");

        MvcResult transactionResult = createTransaction(
                ownerToken,
                ownerCategoryId,
                "EXPENSE",
                "20.00",
                "Dinner",
                "2024-04-11"
        )
                .andExpect(status().isCreated())
                .andReturn();
        long ownerTransactionId = objectMapper
                .readTree(transactionResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        mockMvc.perform(get("/api/v1/categories/{id}", ownerCategoryId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());

        createTransaction(otherToken, ownerCategoryId, "EXPENSE", "15.00", "Blocked", "2024-04-11")
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/transactions/{id}", ownerTransactionId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());
    }

    private String registerAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }

    private long createCategory(String token, String name, String type) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","type":"%s"}
                                """.formatted(name, type)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asLong();
    }

    private org.springframework.test.web.servlet.ResultActions createTransaction(
            String token,
            long categoryId,
            String type,
            String amount,
            String description,
            String transactionDate
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/transactions")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "categoryId": %d,
                          "type": "%s",
                          "amount": %s,
                          "description": "%s",
                          "transactionDate": "%s"
                        }
                        """.formatted(categoryId, type, amount, description, transactionDate)));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
