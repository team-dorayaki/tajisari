package com.tajisali.property.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.property.domain.Property;
import com.tajisali.property.repository.PropertyRepository;
import com.tajisali.user.domain.User;
import com.tajisali.user.service.AnonymousUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
