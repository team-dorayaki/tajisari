package com.tajisali.property.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.property.domain.Property;
import com.tajisali.property.dto.PropertyPriorityUpdateRequest;
import com.tajisali.property.dto.PropertyPriorityUpdateResponse;
import com.tajisali.property.repository.PropertyRepository;
import com.tajisali.user.domain.User;
import com.tajisali.user.service.AnonymousUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PropertyCommandService {

    private final PropertyRepository propertyRepository;
    private final AnonymousUserService anonymousUserService;

    @Transactional
    public void deleteProperty(Long propertyId, String userKey) {
        User user = anonymousUserService.findExisting(userKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));
        Property property = propertyRepository.findByIdAndUserId(propertyId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));

        propertyRepository.delete(property);
    }

    @Transactional
    public PropertyPriorityUpdateResponse updatePriorities(
            PropertyPriorityUpdateRequest request,
            String userKey) {
        validateDistinctProperties(request);

        User user = anonymousUserService.findExisting(userKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));
        Property firstPriority = findOwnedProperty(
                request.firstPriorityPropertyId(), user.getId());
        Property secondPriority = findOwnedProperty(
                request.secondPriorityPropertyId(), user.getId());

        propertyRepository.findAllByUserId(user.getId())
                .forEach(property -> property.updatePriorityRank(null));
        propertyRepository.flush();

        List<PropertyPriorityUpdateResponse.Priority> priorities = new ArrayList<>();
        assignPriority(firstPriority, 1, priorities);
        assignPriority(secondPriority, 2, priorities);

        return new PropertyPriorityUpdateResponse(List.copyOf(priorities));
    }

    private void validateDistinctProperties(PropertyPriorityUpdateRequest request) {
        if (request.firstPriorityPropertyId() != null
                && Objects.equals(
                request.firstPriorityPropertyId(), request.secondPriorityPropertyId())) {
            throw new BusinessException(ErrorCode.PROPERTY_DUPLICATE_PRIORITY);
        }
    }

    private Property findOwnedProperty(Long propertyId, Long userId) {
        if (propertyId == null) {
            return null;
        }
        return propertyRepository.findByIdAndUserId(propertyId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));
    }

    private void assignPriority(
            Property property,
            int priorityRank,
            List<PropertyPriorityUpdateResponse.Priority> priorities) {
        if (property == null) {
            return;
        }
        property.updatePriorityRank(priorityRank);
        priorities.add(new PropertyPriorityUpdateResponse.Priority(property.getId(), priorityRank));
    }
}
