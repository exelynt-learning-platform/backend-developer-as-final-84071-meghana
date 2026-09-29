package com.booking.resourcebooking.service;

import com.booking.resourcebooking.dto.PagedResponse;
import com.booking.resourcebooking.dto.ReservationRequest;
import com.booking.resourcebooking.dto.ReservationResponse;
import com.booking.resourcebooking.dto.ReservationUpdateRequest;
import com.booking.resourcebooking.exception.BadRequestException;
import com.booking.resourcebooking.exception.ConflictException;
import com.booking.resourcebooking.exception.ForbiddenException;
import com.booking.resourcebooking.exception.ResourceNotFoundException;
import com.booking.resourcebooking.model.Reservation;
import com.booking.resourcebooking.model.ReservationStatus;
import com.booking.resourcebooking.model.Resource;
import com.booking.resourcebooking.model.User;
import com.booking.resourcebooking.repository.ReservationRepository;
import com.booking.resourcebooking.repository.ReservationSpecification;
import com.booking.resourcebooking.repository.ResourceRepository;
import com.booking.resourcebooking.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            ResourceRepository resourceRepository,
            UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request, String currentUsername) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + request.getResourceId()));

        if (!resource.isAvailable()) {
            throw new BadRequestException("Resource is currently unavailable for booking");
        }

        validateReservationTimes(request.getStartTime(), request.getEndTime());

        // Check for conflicting overlapping reservations
        checkReservationOverlap(resource.getId(), request.getStartTime(), request.getEndTime(), null);

        // Calculate or validate price
        BigDecimal finalPrice = calculatePrice(resource, request.getStartTime(), request.getEndTime(), request.getPrice());

        ReservationStatus initialStatus = request.getStatus() != null ? request.getStatus() : ReservationStatus.PENDING;

        Reservation reservation = new Reservation(
                user,
                resource,
                request.getStartTime(),
                request.getEndTime(),
                finalPrice,
                initialStatus
        );

        Reservation saved = reservationRepository.save(reservation);
        return ReservationResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReservationResponse> getReservations(
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String sortDir,
            String currentUsername,
            boolean isAdmin) {

        Long userIdFilter = null;
        if (!isAdmin) {
            User user = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));
            userIdFilter = user.getId();
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortProperty = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        Specification<Reservation> spec = ReservationSpecification.filterBy(status, minPrice, maxPrice, userIdFilter);
        Page<Reservation> reservationPage = reservationRepository.findAll(spec, pageable);

        Page<ReservationResponse> responsePage = reservationPage.map(ReservationResponse::fromEntity);
        return PagedResponse.fromPage(responsePage);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id, String currentUsername, boolean isAdmin) {
        Reservation reservation = findReservationById(id);
        validateOwnership(reservation, currentUsername, isAdmin);
        return ReservationResponse.fromEntity(reservation);
    }

    @Transactional
    public ReservationResponse updateReservation(
            Long id,
            ReservationUpdateRequest request,
            String currentUsername,
            boolean isAdmin) {

        Reservation reservation = findReservationById(id);
        validateOwnership(reservation, currentUsername, isAdmin);

        LocalDateTime newStart = request.getStartTime() != null ? request.getStartTime() : reservation.getStartTime();
        LocalDateTime newEnd = request.getEndTime() != null ? request.getEndTime() : reservation.getEndTime();

        // If times are changing
        boolean timesChanged = (request.getStartTime() != null && !request.getStartTime().isEqual(reservation.getStartTime()))
                || (request.getEndTime() != null && !request.getEndTime().isEqual(reservation.getEndTime()));

        if (timesChanged) {
            validateReservationTimes(newStart, newEnd);
            checkReservationOverlap(reservation.getResource().getId(), newStart, newEnd, reservation.getId());
            reservation.setStartTime(newStart);
            reservation.setEndTime(newEnd);

            if (request.getPrice() == null) {
                // recalculate price based on new times
                BigDecimal recalculated = calculatePrice(reservation.getResource(), newStart, newEnd, null);
                reservation.setPrice(recalculated);
            }
        }

        if (request.getPrice() != null) {
            if (!isAdmin) {
                throw new ForbiddenException("Only administrators can manually modify the reservation price");
            }
            if (request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Price must be greater than zero");
            }
            reservation.setPrice(request.getPrice().setScale(2, RoundingMode.HALF_UP));
        }

        if (request.getStatus() != null) {
            if (!isAdmin && request.getStatus() != ReservationStatus.CANCELLED) {
                throw new ForbiddenException("Regular users can only cancel reservations. Only administrators can confirm or re-pend reservations.");
            }
            reservation.setStatus(request.getStatus());
        }

        Reservation updated = reservationRepository.save(reservation);
        return ReservationResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteReservation(Long id, String currentUsername, boolean isAdmin) {
        Reservation reservation = findReservationById(id);
        validateOwnership(reservation, currentUsername, isAdmin);
        reservationRepository.delete(reservation);
    }

    public Reservation findReservationById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }

    private void validateOwnership(Reservation reservation, String currentUsername, boolean isAdmin) {
        if (!isAdmin && !reservation.getUser().getUsername().equals(currentUsername)) {
            throw new ForbiddenException("Access denied: You do not own this reservation");
        }
    }

    private void validateReservationTimes(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new BadRequestException("Start time and end time are required");
        }

        if (!startTime.isBefore(endTime)) {
            throw new BadRequestException("Start time must be strictly before end time");
        }

        // Allow 5 minutes margin for clock skew
        if (startTime.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new BadRequestException("Reservation start time cannot be in the past");
        }
    }

    private void checkReservationOverlap(Long resourceId, LocalDateTime startTime, LocalDateTime endTime, Long excludeId) {
        List<ReservationStatus> activeStatuses = List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        List<Reservation> conflicts = reservationRepository.findOverlappingReservations(
                resourceId, startTime, endTime, activeStatuses, excludeId);

        if (!conflicts.isEmpty()) {
            throw new ConflictException("The selected resource is already booked for this time interval");
        }
    }

    private BigDecimal calculatePrice(Resource resource, LocalDateTime startTime, LocalDateTime endTime, BigDecimal providedPrice) {
        if (providedPrice != null) {
            if (providedPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Reservation price must be greater than zero");
            }
            return providedPrice.setScale(2, RoundingMode.HALF_UP);
        }

        long minutes = Duration.between(startTime, endTime).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(Math.max(minutes, 60))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return resource.getPricePerHour().multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }
}
