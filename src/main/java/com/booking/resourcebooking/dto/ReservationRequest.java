package com.booking.resourcebooking.dto;

import com.booking.resourcebooking.model.ReservationStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReservationRequest {

    @NotNull(message = "Resource ID is required")
    private Long resourceId;

    @NotNull(message = "Start time is required")
    @FutureOrPresent(message = "Start time cannot be in the past")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price; // Optional: if omitted, calculated automatically from resource price per hour and duration

    private ReservationStatus status; // Optional: defaults to PENDING

    public ReservationRequest() {
    }

    public ReservationRequest(Long resourceId, LocalDateTime startTime, LocalDateTime endTime) {
        this.resourceId = resourceId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public ReservationRequest(Long resourceId, LocalDateTime startTime, LocalDateTime endTime, BigDecimal price, ReservationStatus status) {
        this.resourceId = resourceId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
        this.status = status;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
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
}
