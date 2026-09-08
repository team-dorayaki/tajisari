package com.tajisali.property.repository;

import com.tajisali.property.domain.Property;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    @EntityGraph(attributePaths = "images")
    List<Property> findAllByOrderByCreatedAtDesc();
}
