package com.tajisali.property.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.property.domain.Property;
import com.tajisali.property.domain.PropertyAiAnalysis;
import com.tajisali.property.domain.PropertyCostItem;
import com.tajisali.property.dto.PropertyConfirmRequest;
import com.tajisali.property.dto.PropertyConfirmResponse;
import com.tajisali.property.dto.PropertyPriorityUpdateRequest;
import com.tajisali.property.dto.PropertyPriorityUpdateResponse;
import com.tajisali.property.repository.PropertyAiAnalysisRepository;
import com.tajisali.property.repository.PropertyCostItemRepository;
import com.tajisali.property.repository.PropertyImageRepository;
import com.tajisali.property.repository.PropertyRepository;
import com.tajisali.user.domain.User;
import com.tajisali.user.service.AnonymousUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PropertyCommandService {

    private final PropertyRepository propertyRepository;
    private final PropertyAiAnalysisRepository propertyAiAnalysisRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final PropertyCostItemRepository propertyCostItemRepository;
    private final AnonymousUserService anonymousUserService;
    private final PropertyCostCalculationService propertyCostCalculationService;

    @Transactional
    public PropertyConfirmResult confirmUrl(PropertyConfirmRequest request, String userKey) {
        User user = anonymousUserService.resolveOrCreate(userKey);
        LocalDateTime createdAt = LocalDateTime.now();
        Property property = createProperty(user, request, createdAt);

        List<PropertyCostItem> costItems = request.propertyCostItems().stream()
                .map(costItem -> new PropertyCostItem(
                        property,
                        costItem.rawName(),
                        costItem.displayName(),
                        costItem.amount(),
                        costItem.rawValue(),
                        costItem.obligationStatus(),
                        costItem.includedInCalculation(),
                        costItem.timing(),
                        createdAt))
                .toList();
        property.addCostItems(costItems);
        PropertyCostCalculationResult costCalculation = propertyCostCalculationService.calculate(property);
        property.confirmCosts(costCalculation.initialCost(), costCalculation.monthlyCost());

        Property savedProperty = propertyRepository.save(property);
        propertyCostItemRepository.saveAll(costItems);
        propertyAiAnalysisRepository.save(new PropertyAiAnalysis(
                savedProperty,
                request.sourceType().name(),
                request.rawResult().toString(),
                request.modelVersion(),
                createdAt));

        return new PropertyConfirmResult(
                new PropertyConfirmResponse(savedProperty.getId()), user.getUserKey());
    }

    @Transactional
    public void deleteProperty(Long propertyId, String userKey) {
        User user = anonymousUserService.findExisting(userKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));
        Property property = propertyRepository.findOwnedById(propertyId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));

        propertyAiAnalysisRepository.deleteAllByPropertyId(propertyId);
        propertyImageRepository.deleteAllByPropertyId(propertyId);
        propertyCostItemRepository.deleteAllByPropertyId(propertyId);
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

    private Property createProperty(
            User user,
            PropertyConfirmRequest request,
            LocalDateTime createdAt) {
        PropertyConfirmRequest.PropertyInfo property = request.property();
        return new Property(
                user,
                property.sourceSite().name(),
                property.sourceUrl(),
                property.propertyName(),
                property.prefecture(),
                property.city(),
                property.exclusiveAreaM2(),
                property.nearestStation(),
                property.walkMinutes(),
                property.rent(),
                property.managementFee(),
                property.deposit(),
                property.keyMoney(),
                property.availableFrom(),
                property.contractPeriodMonths(),
                property.listedInitialCostTotal(),
                createdAt);
    }

    private Property findOwnedProperty(Long propertyId, Long userId) {
        if (propertyId == null) {
            return null;
        }
        return propertyRepository.findOwnedById(propertyId, userId)
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
