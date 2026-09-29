package com.booking.resourcebooking.repository;

import com.booking.resourcebooking.model.Reservation;
import com.booking.resourcebooking.model.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    Page<Reservation> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT r FROM Reservation r WHERE r.resource.id = :resourceId " +
           "AND r.status IN :activeStatuses " +
           "AND r.startTime < :endTime " +
           "AND r.endTime > :startTime " +
           "AND (:excludeReservationId IS NULL OR r.id != :excludeReservationId)")
    List<Reservation> findOverlappingReservations(
            @Param("resourceId") Long resourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("activeStatuses") Collection<ReservationStatus> activeStatuses,
            @Param("excludeReservationId") Long excludeReservationId
    );
}
