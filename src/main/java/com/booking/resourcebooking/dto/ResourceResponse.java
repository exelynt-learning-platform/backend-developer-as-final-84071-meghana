package com.booking.resourcebooking.dto;

import com.booking.resourcebooking.model.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ResourceResponse {

    private Long id;
    private String name;
    private String description;
    private String type;
    private BigDecimal pricePerHour;
    private boolean available;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ResourceResponse() {
    }

    public static ResourceResponse fromEntity(Resource resource) {
        ResourceResponse res = new ResourceResponse();
        res.setId(resource.getId());
        res.setName(resource.getName());
        res.setDescription(resource.getDescription());
        res.setType(resource.getType());
        res.setPricePerHour(resource.getPricePerHour());
        res.setAvailable(resource.isAvailable());
        res.setCreatedAt(resource.getCreatedAt());
        res.setUpdatedAt(resource.getUpdatedAt());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(BigDecimal pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
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
