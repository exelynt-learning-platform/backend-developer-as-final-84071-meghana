package com.booking.resourcebooking.dto;

import com.booking.resourcebooking.model.ReservationStatus;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReservationUpdateRequest {

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    private ReservationStatus status;

    public ReservationUpdateRequest() {
    }

    public ReservationUpdateRequest(LocalDateTime startTime, LocalDateTime endTime, BigDecimal price, ReservationStatus status) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
        this.status = status;
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
