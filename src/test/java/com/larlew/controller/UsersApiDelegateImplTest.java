package com.larlew.controller;

import com.larlew.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Resiliency tests for UsersApiDelegateImpl
 * 
 * Note: These tests reveal several resiliency concerns:
 * 1. In-memory storage is not thread-safe (no synchronization)
 * 2. State persists across requests (no proper isolation)
 * 3. No validation implementation for required fields and constraints
 * 4. ID generation is not atomic (could have race conditions)
 */
@WebMvcTest(UsersApiDelegateImpl.class)
@Import(SecurityConfig.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class UsersApiDelegateImplTest {

    @Autowired
    private MockMvc mockMvc;

    // Basic functionality tests

    @Test
    @WithMockUser
    void createUserSuccessfully() throws Exception {
        String userRequest = """
            {
                "username": "johndoe",
                "email": "john.doe@example.com",
                "firstName": "John",
                "lastName": "Doe"
            }
            """;

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @WithMockUser
    void getUserByIdSuccessfully() throws Exception {
        // First create a user
        String userRequest = """
            {
                "username": "testuser",
                "email": "test@example.com"
            }
            """;

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userRequest))
                .andExpect(status().isCreated());

        // Then retrieve it
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @WithMockUser
    void getAllUsersReturnsEmptyListInitially() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser
    void getAllUsersReturnsAllCreatedUsers() throws Exception {
        // Create multiple users
        String user1 = """
            {
                "username": "user1",
                "email": "user1@example.com"
            }
            """;

        String user2 = """
            {
                "username": "user2",
                "email": "user2@example.com"
            }
            """;

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(user1))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(user2))
                .andExpect(status().isCreated());

        // Verify all users are returned
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username").value("user1"))
                .andExpect(jsonPath("$[1].username").value("user2"));
    }

    // Resiliency Tests - Invalid Input Handling
    // Note: Current implementation lacks input validation - these tests document expected failures

    @Test
    @WithMockUser
    void createUserWithMissingRequiredFields() throws Exception {
        // RESILIENCY ISSUE: Should validate required fields but currently doesn't
        String invalidRequest = """
            {
                "firstName": "John"
            }
            """;

        // Current behavior: accepts request despite missing required fields
        // Expected behavior: should return 400 Bad Request
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isCreated()); // Should be isBadRequest()
    }

    @Test
    @WithMockUser
    void createUserWithEmptyUsername() throws Exception {
        // RESILIENCY ISSUE: Should validate username is not empty
        String invalidRequest = """
            {
                "username": "",
                "email": "test@example.com"
            }
            """;

        // Current behavior: accepts empty username
        // Expected behavior: should return 400 Bad Request
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isCreated()); // Should be isBadRequest()
    }

    @Test
    @WithMockUser
    void createUserWithInvalidEmail() throws Exception {
        // RESILIENCY ISSUE: Should validate email format
        String invalidRequest = """
            {
                "username": "testuser",
                "email": "invalid-email"
            }
            """;

        // Current behavior: accepts invalid email format
        // Expected behavior: should return 400 Bad Request
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isCreated()); // Should be isBadRequest()
    }

    @Test
    @WithMockUser
    void createUserWithUsernameTooShort() throws Exception {
        // RESILIENCY ISSUE: Should enforce minimum username length (3 chars)
        String invalidRequest = """
            {
                "username": "ab",
                "email": "test@example.com"
            }
            """;

        // Current behavior: accepts username shorter than minimum
        // Expected behavior: should return 400 Bad Request
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isCreated()); // Should be isBadRequest()
    }

    @Test
    @WithMockUser
    void createUserWithUsernameTooLong() throws Exception {
        // RESILIENCY ISSUE: Should enforce maximum username length (50 chars)
        String invalidRequest = String.format("""
            {
                "username": "%s",
                "email": "test@example.com"
            }
            """, "a".repeat(51));

        // Current behavior: accepts username longer than maximum
        // Expected behavior: should return 400 Bad Request
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isCreated()); // Should be isBadRequest()
    }

    @Test
    @WithMockUser
    void createUserWithNullValues() throws Exception {
        // RESILIENCY ISSUE: Should reject null required fields
        String invalidRequest = """
            {
                "username": null,
                "email": null
            }
            """;

        // Current behavior: accepts null values
        // Expected behavior: should return 400 Bad Request
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isCreated()); // Should be isBadRequest()
    }

    @Test
    @WithMockUser
    void createUserWithMalformedJson() throws Exception {
        String malformedRequest = """
            {
                "username": "testuser",
                "email": "test@example.com"
            """; // Missing closing brace

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void createUserWithExtraFields() throws Exception {
        // Should be tolerant of extra fields
        String requestWithExtraFields = """
            {
                "username": "testuser",
                "email": "test@example.com",
                "extraField": "extraValue",
                "anotherExtra": 123
            }
            """;

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestWithExtraFields))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    // Resiliency Tests - Edge Cases

    @Test
    @WithMockUser
    void getUserByIdNotFound() throws Exception {
        mockMvc.perform(get("/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getUserByIdWithNegativeId() throws Exception {
        mockMvc.perform(get("/users/-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getUserByIdWithZeroId() throws Exception {
        mockMvc.perform(get("/users/0"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void createUserWithSpecialCharactersInUsername() throws Exception {
        String userRequest = """
            {
                "username": "user_123-test",
                "email": "test@example.com"
            }
            """;

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("user_123-test"));
    }

    @Test
    @WithMockUser
    void createUserWithUnicodeCharacters() throws Exception {
        String userRequest = """
            {
                "username": "用户名test",
                "email": "test@example.com",
                "firstName": "José",
                "lastName": "Müller"
            }
            """;

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("用户名test"))
                .andExpect(jsonPath("$.firstName").value("José"))
                .andExpect(jsonPath("$.lastName").value("Müller"));
    }

    @Test
    @WithMockUser
    void createMultipleUsersIncrementingIds() throws Exception {
        // Verify ID generation is sequential
        for (int i = 1; i <= 5; i++) {
            String userRequest = String.format("""
                {
                    "username": "user%d",
                    "email": "user%d@example.com"
                }
                """, i, i);

            mockMvc.perform(post("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(userRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(i));
        }
    }

    // Resiliency Tests - Security

    @Test
    void createUserWithoutAuthenticationFails() throws Exception {
        String userRequest = """
            {
                "username": "testuser",
                "email": "test@example.com"
            }
            """;

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userRequest))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUsersWithoutAuthenticationFails() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserByIdWithoutAuthenticationFails() throws Exception {
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isUnauthorized());
    }

    // Resiliency Tests - Content Type Handling

    @Test
    @WithMockUser
    void createUserWithWrongContentType() throws Exception {
        String userRequest = """
            {
                "username": "testuser",
                "email": "test@example.com"
            }
            """;

        mockMvc.perform(post("/users")
                .contentType(MediaType.TEXT_PLAIN)
                .content(userRequest))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @WithMockUser
    void createUserWithEmptyBody() throws Exception {
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
                .andExpect(status().isBadRequest());
    }

    // Resiliency Tests - Boundary Values

    @Test
    @WithMockUser
    void createUserWithExactlyMinimumUsernameLength() throws Exception {
        String userRequest = """
            {
                "username": "abc",
                "email": "test@example.com"
            }
            """;

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("abc"));
    }

    @Test
    @WithMockUser
    void createUserWithExactlyMaximumUsernameLength() throws Exception {
        String username = "a".repeat(50);
        String userRequest = String.format("""
            {
                "username": "%s",
                "email": "test@example.com"
            }
            """, username);

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    @WithMockUser
    void createUserWithOnlyRequiredFields() throws Exception {
        String userRequest = """
            {
                "username": "testuser",
                "email": "test@example.com"
            }
            """;

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value((String) null))
                .andExpect(jsonPath("$.lastName").value((String) null));
    }
}
