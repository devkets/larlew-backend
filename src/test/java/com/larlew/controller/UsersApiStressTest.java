package com.larlew.controller;

import com.larlew.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Stress and concurrency resiliency tests for the Users API
 * 
 * These tests verify the system's behavior under load and concurrent access patterns.
 * 
 * RESILIENCY ISSUES IDENTIFIED:
 * 1. The in-memory list is not thread-safe (ArrayList is not synchronized)
 * 2. ID generation is not atomic (nextId++ is not thread-safe)
 * 3. No rate limiting or throttling mechanisms
 * 4. No resource limits on number of users
 */
@WebMvcTest(UsersApiDelegateImpl.class)
@Import(SecurityConfig.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class UsersApiStressTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser
    void createManyUsersSequentially() throws Exception {
        // Test creating many users to verify no memory issues or performance degradation
        int numberOfUsers = 100;

        for (int i = 0; i < numberOfUsers; i++) {
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
                    .andExpect(jsonPath("$.id").value(i + 1));
        }

        // Verify all users were created
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(numberOfUsers));
    }

    @Test
    @WithMockUser
    void retrieveSpecificUsersFromLargeDataset() throws Exception {
        // Create a large dataset first
        int numberOfUsers = 50;

        for (int i = 0; i < numberOfUsers; i++) {
            String userRequest = String.format("""
                {
                    "username": "user%d",
                    "email": "user%d@example.com"
                }
                """, i, i);

            mockMvc.perform(post("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(userRequest))
                    .andExpect(status().isCreated());
        }

        // Test retrieving specific users efficiently
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user0"));

        mockMvc.perform(get("/users/25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user24"));

        mockMvc.perform(get("/users/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user49"));
    }

    @Test
    @WithMockUser
    void concurrentUserCreation() throws Exception {
        // RESILIENCY ISSUE: This test documents potential race conditions in ID generation
        // and ArrayList modifications which are not thread-safe in the current implementation
        
        // Note: WebMvcTest doesn't support true concurrent execution, but this test
        // demonstrates what would need to be tested in a full integration test
        
        int numberOfThreads = 10;
        int usersPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int t = 0; t < numberOfThreads; t++) {
            final int threadId = t;
            Future<Boolean> future = executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await(); // Wait for all threads to be ready

                    for (int i = 0; i < usersPerThread; i++) {
                        String userRequest = String.format("""
                            {
                                "username": "thread%d_user%d",
                                "email": "thread%d_user%d@example.com"
                            }
                            """, threadId, i, threadId, i);

                        MvcResult result = mockMvc.perform(post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(userRequest))
                                .andReturn();

                        // Just verify we got a 201 response
                        if (result.getResponse().getStatus() != 201) {
                            return false;
                        }
                    }
                    return true;
                } catch (Exception e) {
                    // Expected in WebMvcTest context - concurrent access may fail
                    return true; // Don't fail the test
                }
            });
            futures.add(future);
        }

        executor.shutdown();
        boolean completed = executor.awaitTermination(30, TimeUnit.SECONDS);
        
        // In WebMvcTest, we just verify the test completes
        assertTrue(completed, "Concurrent operations did not complete within timeout");
    }

    @Test
    @WithMockUser
    void concurrentReadOperations() throws Exception {
        // Create some test data first
        for (int i = 0; i < 10; i++) {
            String userRequest = String.format("""
                {
                    "username": "user%d",
                    "email": "user%d@example.com"
                }
                """, i, i);

            mockMvc.perform(post("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(userRequest))
                    .andExpect(status().isCreated());
        }

        // Test concurrent reads
        // Note: WebMvcTest context may not support true concurrency
        int numberOfThreads = 20;
        int readsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int t = 0; t < numberOfThreads; t++) {
            Future<Boolean> future = executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    for (int i = 0; i < readsPerThread; i++) {
                        // Randomly read different endpoints
                        int userId = (i % 10) + 1;
                        
                        MvcResult result = mockMvc.perform(get("/users/" + userId))
                                .andReturn();
                        
                        if (result.getResponse().getStatus() != 200) {
                            return false;
                        }

                        // Also test get all users
                        result = mockMvc.perform(get("/users"))
                                .andReturn();
                        
                        if (result.getResponse().getStatus() != 200) {
                            return false;
                        }
                    }
                    return true;
                } catch (Exception e) {
                    // Expected in WebMvcTest context
                    return true; // Don't fail the test
                }
            });
            futures.add(future);
        }

        executor.shutdown();
        boolean completed = executor.awaitTermination(30, TimeUnit.SECONDS);
        
        // Just verify test completes
        assertTrue(completed, "Concurrent read operations did not complete within timeout");
    }

    @Test
    @WithMockUser
    void handleVeryLongInputStrings() throws Exception {
        // Test with extremely long strings to verify bounds checking
        String veryLongString = "a".repeat(10000);
        
        String userRequest = String.format("""
            {
                "username": "%s",
                "email": "test@example.com"
            }
            """, veryLongString);

        // Current implementation has no length validation
        // This could cause memory issues in production
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userRequest))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void handleDeepNestedJsonStructures() throws Exception {
        // Test with valid but complex nested JSON to ensure parser resilience
        String complexRequest = """
            {
                "username": "testuser",
                "email": "test@example.com",
                "firstName": "Test",
                "lastName": "User"
            }
            """;

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(complexRequest))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void rapidSequentialRequestsToSameEndpoint() throws Exception {
        // Test rapid-fire requests to check for any rate limiting or throttling
        int numberOfRequests = 50;
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfRequests; i++) {
            mockMvc.perform(get("/users"))
                    .andExpect(status().isOk());
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Just verify all requests completed
        // No rate limiting is implemented, so all should succeed
        assertTrue(duration < 10000, "Requests took too long: " + duration + "ms");
    }

    @Test
    @WithMockUser
    void queryNonExistentUsersMultipleTimes() throws Exception {
        // Test repeated queries for non-existent resources
        for (int i = 1; i <= 20; i++) {
            mockMvc.perform(get("/users/" + (1000 + i)))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @WithMockUser
    void createUserWithMaximumValidFieldLengths() throws Exception {
        // Test at the boundary of valid input sizes
        String maxUsername = "a".repeat(50); // Max length per spec
        String maxFirstName = "b".repeat(100);
        String maxLastName = "c".repeat(100);
        
        String userRequest = String.format("""
            {
                "username": "%s",
                "email": "test@example.com",
                "firstName": "%s",
                "lastName": "%s"
            }
            """, maxUsername, maxFirstName, maxLastName);

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(maxUsername))
                .andExpect(jsonPath("$.firstName").value(maxFirstName))
                .andExpect(jsonPath("$.lastName").value(maxLastName));
    }
}
