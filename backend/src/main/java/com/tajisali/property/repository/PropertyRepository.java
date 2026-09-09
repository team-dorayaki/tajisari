package com.tajisali.property.repository;

import com.tajisali.property.domain.Property;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    @EntityGraph(attributePaths = "images")
    List<Property> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "images")
    Optional<Property> findByIdAndUserId(Long propertyId, Long userId);
}
