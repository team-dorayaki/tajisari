package com.tajisali.property.repository;

import com.tajisali.property.domain.Property;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    @EntityGraph(attributePaths = "images")
    List<Property> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "images")
    Optional<Property> findByIdAndUserId(Long propertyId, Long userId);

    @EntityGraph(attributePaths = "images")
    List<Property> findAllByIdInAndUserId(List<Long> propertyIds, Long userId);

    @Query("""
            select property
            from Property property
            where property.id = :propertyId
              and property.user.id = :userId
            """)
    Optional<Property> findOwnedById(Long propertyId, Long userId);

    List<Property> findAllByUserId(Long userId);
}
