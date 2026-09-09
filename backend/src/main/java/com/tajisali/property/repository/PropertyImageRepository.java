package com.tajisali.property.repository;

import com.tajisali.property.domain.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {

    @Modifying
    @Query("delete from PropertyImage image where image.property.id = :propertyId")
    int deleteAllByPropertyId(Long propertyId);
}
