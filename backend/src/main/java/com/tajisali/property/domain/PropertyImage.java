package com.tajisali.property.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "property_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "image_order", nullable = false)
    private int imageOrder;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PropertyImage(
            Property property,
            String storageKey,
            int imageOrder,
            String originalFilename,
            LocalDateTime createdAt) {
        this.property = Objects.requireNonNull(property, "property must not be null");
        this.storageKey = Objects.requireNonNull(storageKey, "storageKey must not be null");
        this.imageOrder = imageOrder;
        this.originalFilename = originalFilename;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
