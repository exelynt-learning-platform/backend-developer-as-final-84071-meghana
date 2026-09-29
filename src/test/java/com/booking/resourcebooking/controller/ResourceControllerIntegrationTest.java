package com.booking.resourcebooking.controller;

import com.booking.resourcebooking.dto.ResourceRequest;
import com.booking.resourcebooking.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ResourceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        adminToken = "Bearer " + jwtTokenProvider.generateTokenFromUsername("admin", "ROLE_ADMIN");
        userToken = "Bearer " + jwtTokenProvider.generateTokenFromUsername("user1", "ROLE_USER");
    }

    @Test
    @DisplayName("GET /api/resources - 401 Unauthorized when unauthenticated")
    void testGetAllResources_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/resources - 200 OK for regular USER (read-only access)")
    void testGetAllResources_AsUser() throws Exception {
        mockMvc.perform(get("/api/resources")
                        .header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    @DisplayName("GET /api/resources/{id} - 200 OK for regular USER")
    void testGetResourceById_AsUser() throws Exception {
        mockMvc.perform(get("/api/resources/1")
                        .header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /api/resources - 403 Forbidden when regular USER attempts to create resource")
    void testCreateResource_ForbiddenForUser() throws Exception {
        ResourceRequest request = new ResourceRequest(
                "Unauthorized Room",
                "Description",
                "ROOM",
                new BigDecimal("60.00"),
                true
        );

        mockMvc.perform(post("/api/resources")
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/resources - 201 Created when ADMIN creates resource")
    void testCreateResource_SuccessForAdmin() throws Exception {
        ResourceRequest request = new ResourceRequest(
                "Podcast Studio Deluxe",
                "Soundproof recording studio with Rodecaster Pro II",
                "STUDIO",
                new BigDecimal("45.00"),
                true
        );

        mockMvc.perform(post("/api/resources")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Podcast Studio Deluxe"))
                .andExpect(jsonPath("$.pricePerHour").value(45.00));
    }

    @Test
    @DisplayName("PUT /api/resources/{id} - 403 Forbidden for USER; 200 OK for ADMIN")
    void testUpdateResource_Rbac() throws Exception {
        ResourceRequest updateRequest = new ResourceRequest(
                "Updated Room Name",
                "Updated Description",
                "ROOM",
                new BigDecimal("70.00"),
                true
        );

        // USER attempt -> 403 Forbidden
        mockMvc.perform(put("/api/resources/1")
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        // ADMIN attempt -> 200 OK
        mockMvc.perform(put("/api/resources/1")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Room Name"));
    }

    @Test
    @DisplayName("DELETE /api/resources/{id} - 403 Forbidden for USER")
    void testDeleteResource_ForbiddenForUser() throws Exception {
        mockMvc.perform(delete("/api/resources/2")
                        .header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/resources - Validation failure for negative price")
    void testCreateResource_ValidationFailure() throws Exception {
        ResourceRequest request = new ResourceRequest(
                "Invalid Resource",
                "Description",
                "ROOM",
                new BigDecimal("-10.00"), // negative price
                true
        );

        mockMvc.perform(post("/api/resources")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.pricePerHour").exists());
    }
}
