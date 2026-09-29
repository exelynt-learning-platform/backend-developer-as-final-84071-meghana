package com.booking.resourcebooking.controller;

import com.booking.resourcebooking.dto.ReservationRequest;
import com.booking.resourcebooking.dto.ReservationUpdateRequest;
import com.booking.resourcebooking.model.ReservationStatus;
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
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ReservationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() {
        adminToken = "Bearer " + jwtTokenProvider.generateTokenFromUsername("admin", "ROLE_ADMIN");
        user1Token = "Bearer " + jwtTokenProvider.generateTokenFromUsername("user1", "ROLE_USER");
        user2Token = "Bearer " + jwtTokenProvider.generateTokenFromUsername("user2", "ROLE_USER");
    }

    @Test
    @DisplayName("POST /api/reservations - User creates reservation with user identity from JWT")
    void testCreateReservation_Success() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(10).withHour(10).withMinute(0);
        LocalDateTime end = LocalDateTime.now().plusDays(10).withHour(12).withMinute(0);

        ReservationRequest request = new ReservationRequest(1L, start, end);

        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("user1")) // taken from JWT
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.price").value(100.00)); // 2 hours * 50.00
    }

    @Test
    @DisplayName("POST /api/reservations - Validation failure when start time is after end time")
    void testCreateReservation_InvalidTimes() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(5).withHour(15).withMinute(0);
        LocalDateTime end = LocalDateTime.now().plusDays(5).withHour(12).withMinute(0); // before start

        ReservationRequest request = new ReservationRequest(1L, start, end);

        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Start time must be strictly before end time")));
    }

    @Test
    @DisplayName("POST /api/reservations - Conflict 409 when booking overlapping time slot")
    void testCreateReservation_OverlapConflict() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(20).withHour(10).withMinute(0);
        LocalDateTime end = LocalDateTime.now().plusDays(20).withHour(14).withMinute(0);

        ReservationRequest request1 = new ReservationRequest(1L, start, end);

        // First reservation succeeds
        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Second overlapping reservation (11:00 - 13:00) fails with 409 Conflict
        ReservationRequest request2 = new ReservationRequest(1L,
                start.plusHours(1),
                end.minusHours(1));

        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Reservation Ownership - USER1 sees only their own reservations, ADMIN sees all")
    void testReservationOwnership_ListFiltering() throws Exception {
        // User 1 requests list
        mockMvc.perform(get("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[*].username", everyItem(equalTo("user1"))));

        // Admin requests list -> should contain both user1 and user2 reservations
        mockMvc.perform(get("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[*].username", hasItems("user1", "user2")));
    }

    @Test
    @DisplayName("Reservation Ownership - USER1 cannot access USER2's reservation (403 Forbidden)")
    void testReservationOwnership_DirectAccessForbidden() throws Exception {
        // Reservation 4 was seeded for user2
        mockMvc.perform(get("/api/reservations/4")
                        .header(HttpHeaders.AUTHORIZATION, user1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Reservation Ownership - ADMIN can access any reservation (200 OK)")
    void testReservationOwnership_AdminAccessAll() throws Exception {
        mockMvc.perform(get("/api/reservations/4")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.username").value("user2"));
    }

    @Test
    @DisplayName("PUT /api/reservations/{id} - USER can cancel own reservation; cannot confirm")
    void testUpdateReservation_StatusRbac() throws Exception {
        // USER cancels own reservation 2 (which is PENDING) -> 200 OK
        ReservationUpdateRequest cancelRequest = new ReservationUpdateRequest();
        cancelRequest.setStatus(ReservationStatus.CANCELLED);

        mockMvc.perform(put("/api/reservations/2")
                        .header(HttpHeaders.AUTHORIZATION, user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // USER attempts to confirm reservation -> 403 Forbidden
        ReservationUpdateRequest confirmRequest = new ReservationUpdateRequest();
        confirmRequest.setStatus(ReservationStatus.CONFIRMED);

        mockMvc.perform(put("/api/reservations/2")
                        .header(HttpHeaders.AUTHORIZATION, user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isForbidden());

        // ADMIN can confirm reservation -> 200 OK
        mockMvc.perform(put("/api/reservations/2")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("GET /api/reservations - Filtering by status, minPrice, and maxPrice")
    void testFiltering_StatusAndPrice() throws Exception {
        // Filter by status=CONFIRMED
        mockMvc.perform(get("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status", everyItem(equalTo("CONFIRMED"))));

        // Filter by minPrice=150 and maxPrice=300
        mockMvc.perform(get("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .param("minPrice", "150.00")
                        .param("maxPrice", "300.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())));
    }

    @Test
    @DisplayName("GET /api/reservations - Pagination and Sorting")
    void testPaginationAndSorting() throws Exception {
        // Page 0, size 2, sorted by price descending
        mockMvc.perform(get("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "price")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }
}
