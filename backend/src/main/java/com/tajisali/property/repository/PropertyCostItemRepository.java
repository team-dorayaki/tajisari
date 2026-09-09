package com.tajisali.property.repository;

import com.tajisali.property.domain.PropertyCostItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PropertyCostItemRepository extends JpaRepository<PropertyCostItem, Long> {

    @Modifying
    @Query("delete from PropertyCostItem costItem where costItem.property.id = :propertyId")
    int deleteAllByPropertyId(Long propertyId);
}
