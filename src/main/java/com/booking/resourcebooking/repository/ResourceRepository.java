package com.booking.resourcebooking.repository;

import com.booking.resourcebooking.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByAvailableTrue();
    List<Resource> findByTypeIgnoreCase(String type);
}
