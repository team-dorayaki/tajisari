package com.tajisali.property.repository;

import com.tajisali.property.domain.PropertyAiAnalysis;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PropertyAiAnalysisRepository extends JpaRepository<PropertyAiAnalysis, Long> {

    Optional<PropertyAiAnalysis> findTopByPropertyIdOrderByIdDesc(Long propertyId);

    @EntityGraph(attributePaths = "property")
    Optional<PropertyAiAnalysis> findDetailById(Long analysisId);

    @Modifying
    @Query("delete from PropertyAiAnalysis analysis where analysis.property.id = :propertyId")
    int deleteAllByPropertyId(Long propertyId);
}
