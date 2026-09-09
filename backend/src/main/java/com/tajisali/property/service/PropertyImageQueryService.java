package com.tajisali.property.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.property.repository.PropertyImageRepository;
import com.tajisali.user.service.AnonymousUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PropertyImageQueryService {

    private final PropertyImageRepository propertyImageRepository;
    private final AnonymousUserService anonymousUserService;
    private final PropertyImageStorageService propertyImageStorageService;

    @Transactional(readOnly = true)
    public PropertyImageStorageService.StoredImageFile getImage(Long imageId, String userKey) {
        var user = anonymousUserService.findExisting(userKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_IMAGE_NOT_FOUND));
        var image = propertyImageRepository.findByIdAndProperty_User_Id(imageId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_IMAGE_NOT_FOUND));
        return propertyImageStorageService.load(image.getStorageKey());
    }
}
