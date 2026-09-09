package com.tajisali.property.repository;

import com.tajisali.property.domain.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {

    Optional<PropertyImage> findByIdAndProperty_User_Id(Long imageId, Long userId);

    @Modifying
    @Query("delete from PropertyImage image where image.property.id = :propertyId")
    int deleteAllByPropertyId(Long propertyId);
}
