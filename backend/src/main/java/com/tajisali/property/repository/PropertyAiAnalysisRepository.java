package com.tajisali.property.repository;

import com.tajisali.property.domain.PropertyAiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyAiAnalysisRepository extends JpaRepository<PropertyAiAnalysis, Long> {

    Optional<PropertyAiAnalysis> findTopByPropertyIdOrderByIdDesc(Long propertyId);
}
