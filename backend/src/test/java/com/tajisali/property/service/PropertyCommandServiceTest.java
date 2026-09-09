package com.tajisali.property.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.property.domain.Property;
import com.tajisali.property.repository.PropertyRepository;
import com.tajisali.user.domain.User;
import com.tajisali.user.service.AnonymousUserService;
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

    private static final String USER_KEY = "00000000-0000-0000-0000-000000000001";

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private AnonymousUserService anonymousUserService;

    private PropertyCommandService propertyCommandService;

    @BeforeEach
    void setUp() {
        propertyCommandService = new PropertyCommandService(
                propertyRepository, anonymousUserService);
    }

    @Test
    void 저장된_매물을_삭제한다() {
        User user = user(1L);
        Property property = new Property(
                "요코하마 스튜디오", 65_000L, 245_000L, 70_000L,
                1, LocalDateTime.now());
        when(anonymousUserService.findExisting(USER_KEY)).thenReturn(Optional.of(user));
        when(propertyRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(property));

        propertyCommandService.deleteProperty(10L, USER_KEY);

        verify(propertyRepository).delete(property);
    }

    @Test
    void 자신의_매물이_아니면_삭제할_수_없다() {
        User user = user(1L);
        when(anonymousUserService.findExisting(USER_KEY)).thenReturn(Optional.of(user));
        when(propertyRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propertyCommandService.deleteProperty(999L, USER_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROPERTY_NOT_FOUND);

        verify(propertyRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 유효한_사용자_쿠키가_없으면_매물을_조회하지_않는다() {
        when(anonymousUserService.findExisting(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propertyCommandService.deleteProperty(10L, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROPERTY_NOT_FOUND);

        verify(propertyRepository, never())
                .findByIdAndUserId(org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong());
        verify(propertyRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    private User user(Long id) {
        User user = new User(USER_KEY);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
