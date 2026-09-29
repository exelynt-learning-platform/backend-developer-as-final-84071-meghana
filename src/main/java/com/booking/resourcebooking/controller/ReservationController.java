package com.booking.resourcebooking.controller;

import com.booking.resourcebooking.dto.PagedResponse;
import com.booking.resourcebooking.dto.ReservationRequest;
import com.booking.resourcebooking.dto.ReservationResponse;
import com.booking.resourcebooking.dto.ReservationUpdateRequest;
import com.booking.resourcebooking.model.ReservationStatus;
import com.booking.resourcebooking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations", description = "Endpoints for creating, managing, filtering, and viewing reservations")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(
            summary = "Create reservation",
            description = "Creates a reservation. Note: User identity is automatically extracted from the JWT token, never accepted from request body."
    )
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest request,
            Authentication authentication) {
        ReservationResponse response = reservationService.createReservation(request, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(
            summary = "Get reservations with filtering, pagination, and sorting",
            description = "ADMIN views all reservations; USER views only their own reservations. Supports filtering by status, minPrice, maxPrice, and pagination/sorting."
    )
    public ResponseEntity<PagedResponse<ReservationResponse>> getReservations(
            @Parameter(description = "Filter by reservation status (PENDING, CONFIRMED, CANCELLED)")
            @RequestParam(value = "status", required = false) ReservationStatus status,

            @Parameter(description = "Filter by minimum reservation price")
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,

            @Parameter(description = "Filter by maximum reservation price")
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,

            @Parameter(description = "Zero-based page number")
            @RequestParam(value = "page", defaultValue = "0") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(value = "size", defaultValue = "10") int size,

            @Parameter(description = "Property to sort by (e.g., id, price, startTime)")
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,

            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir,

            Authentication authentication) {

        boolean isAdmin = checkIsAdmin(authentication);
        PagedResponse<ReservationResponse> reservations = reservationService.getReservations(
                status, minPrice, maxPrice, page, size, sortBy, sortDir, authentication.getName(), isAdmin);

        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get reservation by ID",
            description = "ADMIN can access any reservation; USER can access only their own reservations."
    )
    public ResponseEntity<ReservationResponse> getReservationById(
            @PathVariable Long id,
            Authentication authentication) {
        boolean isAdmin = checkIsAdmin(authentication);
        ReservationResponse reservation = reservationService.getReservationById(id, authentication.getName(), isAdmin);
        return ResponseEntity.ok(reservation);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update reservation",
            description = "ADMIN has full update access (times, price, status); USER can modify times or cancel their own reservation."
    )
    public ResponseEntity<ReservationResponse> updateReservation(
            @PathVariable Long id,
            @RequestBody ReservationUpdateRequest request,
            Authentication authentication) {
        boolean isAdmin = checkIsAdmin(authentication);
        ReservationResponse updated = reservationService.updateReservation(id, request, authentication.getName(), isAdmin);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete reservation",
            description = "ADMIN can delete any reservation; USER can delete only their own reservation."
    )
    public ResponseEntity<Void> deleteReservation(
            @PathVariable Long id,
            Authentication authentication) {
        boolean isAdmin = checkIsAdmin(authentication);
        reservationService.deleteReservation(id, authentication.getName(), isAdmin);
        return ResponseEntity.noContent().build();
    }

    private boolean checkIsAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
    }
}
