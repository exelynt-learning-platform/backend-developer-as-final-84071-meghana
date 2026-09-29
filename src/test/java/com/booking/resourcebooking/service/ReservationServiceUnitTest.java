package com.booking.resourcebooking.service;

import com.booking.resourcebooking.dto.ReservationRequest;
import com.booking.resourcebooking.dto.ReservationResponse;
import com.booking.resourcebooking.exception.BadRequestException;
import com.booking.resourcebooking.exception.ConflictException;
import com.booking.resourcebooking.exception.ForbiddenException;
import com.booking.resourcebooking.model.*;
import com.booking.resourcebooking.repository.ReservationRepository;
import com.booking.resourcebooking.repository.ResourceRepository;
import com.booking.resourcebooking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceUnitTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReservationService reservationService;

    private User sampleUser;
    private Resource sampleResource;

    @BeforeEach
    void setUp() {
        sampleUser = new User("alice", "passHash", "alice@booking.com", Role.ROLE_USER);
        sampleUser.setId(10L);

        sampleResource = new Resource("Meeting Room Alpha", "Equipped room", "ROOM", new BigDecimal("50.00"), true);
        sampleResource.setId(20L);
    }

    @Test
    @DisplayName("createReservation calculates correct price for 3-hour booking")
    void testCreateReservation_PriceCalculation() {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(3);

        ReservationRequest request = new ReservationRequest(20L, start, end);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sampleUser));
        when(resourceRepository.findById(20L)).thenReturn(Optional.of(sampleResource));
        when(reservationRepository.findOverlappingReservations(eq(20L), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation r = invocation.getArgument(0);
            r.setId(99L);
            return r;
        });

        ReservationResponse response = reservationService.createReservation(request, "alice");

        assertNotNull(response);
        assertEquals(new BigDecimal("150.00"), response.getPrice()); // 3 hours * 50.00
        assertEquals(ReservationStatus.PENDING, response.getStatus());
        assertEquals("alice", response.getUsername());
    }

    @Test
    @DisplayName("createReservation throws BadRequestException when start time is equal or after end time")
    void testCreateReservation_InvalidTimes() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0);
        LocalDateTime end = start.minusHours(1);

        ReservationRequest request = new ReservationRequest(20L, start, end);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sampleUser));
        when(resourceRepository.findById(20L)).thenReturn(Optional.of(sampleResource));

        assertThrows(BadRequestException.class, () ->
                reservationService.createReservation(request, "alice"));
    }

    @Test
    @DisplayName("createReservation throws ConflictException when overlap is detected")
    void testCreateReservation_OverlapConflict() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        ReservationRequest request = new ReservationRequest(20L, start, end);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sampleUser));
        when(resourceRepository.findById(20L)).thenReturn(Optional.of(sampleResource));

        Reservation existing = new Reservation(sampleUser, sampleResource, start, end, new BigDecimal("100.00"), ReservationStatus.CONFIRMED);
        when(reservationRepository.findOverlappingReservations(eq(20L), any(), any(), any(), any()))
                .thenReturn(List.of(existing));

        assertThrows(ConflictException.class, () ->
                reservationService.createReservation(request, "alice"));
    }

    @Test
    @DisplayName("getReservationById throws ForbiddenException when user is not owner and not admin")
    void testGetReservationById_OwnershipForbidden() {
        Reservation reservation = new Reservation(sampleUser, sampleResource,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2),
                new BigDecimal("100.00"), ReservationStatus.CONFIRMED);
        reservation.setId(50L);

        when(reservationRepository.findById(50L)).thenReturn(Optional.of(reservation));

        assertThrows(ForbiddenException.class, () ->
                reservationService.getReservationById(50L, "bob", false));
    }
}
