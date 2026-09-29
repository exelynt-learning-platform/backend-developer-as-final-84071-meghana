package com.booking.resourcebooking.dto;

import com.booking.resourcebooking.model.Reservation;
import com.booking.resourcebooking.model.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReservationResponse {

    private Long id;
    private Long resourceId;
    private String resourceName;
    private String resourceType;
    private Long userId;
    private String username;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal price;
    private ReservationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ReservationResponse() {
    }

    public static ReservationResponse fromEntity(Reservation reservation) {
        ReservationResponse res = new ReservationResponse();
        res.setId(reservation.getId());
        if (reservation.getResource() != null) {
            res.setResourceId(reservation.getResource().getId());
            res.setResourceName(reservation.getResource().getName());
            res.setResourceType(reservation.getResource().getType());
        }
        if (reservation.getUser() != null) {
            res.setUserId(reservation.getUser().getId());
            res.setUsername(reservation.getUser().getUsername());
        }
        res.setStartTime(reservation.getStartTime());
        res.setEndTime(reservation.getEndTime());
        res.setPrice(reservation.getPrice());
        res.setStatus(reservation.getStatus());
        res.setCreatedAt(reservation.getCreatedAt());
        res.setUpdatedAt(reservation.getUpdatedAt());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
