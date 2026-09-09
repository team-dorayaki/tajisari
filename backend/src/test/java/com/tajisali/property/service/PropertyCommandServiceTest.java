package com.tajisali.property.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.property.domain.Property;
import com.tajisali.property.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyCommandServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    private PropertyCommandService propertyCommandService;

    @BeforeEach
    void setUp() {
        propertyCommandService = new PropertyCommandService(propertyRepository);
    }

    @Test
    void 저장된_매물을_삭제한다() {
        Property property = new Property(
                "요코하마 스튜디오", 65_000L, 245_000L, 70_000L,
                1, LocalDateTime.now());
        when(propertyRepository.findById(10L)).thenReturn(Optional.of(property));

        propertyCommandService.deleteProperty(10L);

        verify(propertyRepository).delete(property);
    }

    @Test
    void 존재하지_않는_매물은_삭제할_수_없다() {
        when(propertyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propertyCommandService.deleteProperty(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROPERTY_NOT_FOUND);

        verify(propertyRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }
}
