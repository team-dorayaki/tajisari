package com.tajisali.property.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "property_ai_analysis")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyAiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;

    @Column(name = "raw_json", nullable = false, columnDefinition = "json")
    private String rawJson;

    @Column(name = "model_version", length = 100)
    private String modelVersion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PropertyAiAnalysis(
            Property property,
            String sourceType,
            String rawJson,
            String modelVersion,
            LocalDateTime createdAt) {
        this.property = Objects.requireNonNull(property, "property must not be null");
        this.sourceType = sourceType;
        this.rawJson = rawJson;
        this.modelVersion = modelVersion;
        this.createdAt = createdAt;
    }
}
