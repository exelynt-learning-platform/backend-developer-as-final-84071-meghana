package com.booking.resourcebooking.service;

import com.booking.resourcebooking.dto.ResourceRequest;
import com.booking.resourcebooking.dto.ResourceResponse;
import com.booking.resourcebooking.exception.BadRequestException;
import com.booking.resourcebooking.exception.ResourceNotFoundException;
import com.booking.resourcebooking.model.Resource;
import com.booking.resourcebooking.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> getAllResources(Boolean availableOnly) {
        List<Resource> resources;
        if (Boolean.TRUE.equals(availableOnly)) {
            resources = resourceRepository.findByAvailableTrue();
        } else {
            resources = resourceRepository.findAll();
        }
        return resources.stream()
                .map(ResourceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(Long id) {
        Resource resource = findResourceById(id);
        return ResourceResponse.fromEntity(resource);
    }

    public Resource findResourceById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    @Transactional
    public ResourceResponse createResource(ResourceRequest request) {
        if (request.getPricePerHour() == null || request.getPricePerHour().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Resource price per hour must be greater than zero");
        }

        Resource resource = new Resource(
                request.getName(),
                request.getDescription(),
                request.getType(),
                request.getPricePerHour(),
                request.isAvailable()
        );

        Resource savedResource = resourceRepository.save(resource);
        return ResourceResponse.fromEntity(savedResource);
    }

    @Transactional
    public ResourceResponse updateResource(Long id, ResourceRequest request) {
        Resource resource = findResourceById(id);

        if (request.getPricePerHour() != null) {
            if (request.getPricePerHour().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Resource price per hour must be greater than zero");
            }
            resource.setPricePerHour(request.getPricePerHour());
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            resource.setName(request.getName());
        }

        if (request.getDescription() != null) {
            resource.setDescription(request.getDescription());
        }

        if (request.getType() != null && !request.getType().isBlank()) {
            resource.setType(request.getType());
        }

        resource.setAvailable(request.isAvailable());

        Resource updated = resourceRepository.save(resource);
        return ResourceResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteResource(Long id) {
        Resource resource = findResourceById(id);
        resourceRepository.delete(resource);
    }
}
