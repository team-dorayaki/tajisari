package com.tajisali.property.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.property.domain.Property;
import com.tajisali.property.dto.PropertyPriorityUpdateRequest;
import com.tajisali.property.repository.PropertyAiAnalysisRepository;
import com.tajisali.property.repository.PropertyCostItemRepository;
import com.tajisali.property.repository.PropertyImageRepository;
import com.tajisali.property.repository.PropertyRepository;
import com.tajisali.user.domain.User;
import com.tajisali.user.service.AnonymousUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    private PropertyAiAnalysisRepository propertyAiAnalysisRepository;

    @Mock
    private PropertyImageRepository propertyImageRepository;

    @Mock
    private PropertyCostItemRepository propertyCostItemRepository;

    @Mock
    private AnonymousUserService anonymousUserService;

    private PropertyCommandService propertyCommandService;

    @BeforeEach
    void setUp() {
        propertyCommandService = new PropertyCommandService(
                propertyRepository,
                propertyAiAnalysisRepository,
                propertyImageRepository,
                propertyCostItemRepository,
                anonymousUserService);
    }

    @Test
    void 저장된_매물을_삭제한다() {
        User user = user(1L);
        Property property = new Property(
                "요코하마 스튜디오", 65_000L, 245_000L, 70_000L,
                1, LocalDateTime.now());
        when(anonymousUserService.findExisting(USER_KEY)).thenReturn(Optional.of(user));
        when(propertyRepository.findOwnedById(10L, 1L)).thenReturn(Optional.of(property));

        propertyCommandService.deleteProperty(10L, USER_KEY);

        verify(propertyAiAnalysisRepository).deleteAllByPropertyId(10L);
        verify(propertyImageRepository).deleteAllByPropertyId(10L);
        verify(propertyCostItemRepository).deleteAllByPropertyId(10L);
        verify(propertyRepository).delete(property);
    }

    @Test
    void 자신의_매물이_아니면_삭제할_수_없다() {
        User user = user(1L);
        when(anonymousUserService.findExisting(USER_KEY)).thenReturn(Optional.of(user));
        when(propertyRepository.findOwnedById(999L, 1L)).thenReturn(Optional.empty());

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
                .findOwnedById(org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong());
        verify(propertyRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 사용자의_기존_순위를_해제하고_새_순위를_일괄_저장한다() {
        User user = user(1L);
        Property oldFirst = property(1L, 1);
        Property newFirst = property(2L, null);
        Property newSecond = property(3L, 2);
        when(anonymousUserService.findExisting(USER_KEY)).thenReturn(Optional.of(user));
        when(propertyRepository.findOwnedById(2L, 1L)).thenReturn(Optional.of(newFirst));
        when(propertyRepository.findOwnedById(3L, 1L)).thenReturn(Optional.of(newSecond));
        when(propertyRepository.findAllByUserId(1L))
                .thenReturn(List.of(oldFirst, newFirst, newSecond));

        var response = propertyCommandService.updatePriorities(
                new PropertyPriorityUpdateRequest(2L, 3L), USER_KEY);

        assertThat(oldFirst.getPriorityRank()).isNull();
        assertThat(newFirst.getPriorityRank()).isEqualTo(1);
        assertThat(newSecond.getPriorityRank()).isEqualTo(2);
        assertThat(response.priorities())
                .extracting(item -> item.propertyId(), item -> item.priorityRank())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2L, 1),
                        org.assertj.core.groups.Tuple.tuple(3L, 2));
        verify(propertyRepository).flush();
    }

    @Test
    void 우선순위를_null로_보내면_사용자의_기존_순위를_해제한다() {
        User user = user(1L);
        Property oldFirst = property(1L, 1);
        Property oldSecond = property(2L, 2);
        when(anonymousUserService.findExisting(USER_KEY)).thenReturn(Optional.of(user));
        when(propertyRepository.findAllByUserId(1L)).thenReturn(List.of(oldFirst, oldSecond));

        var response = propertyCommandService.updatePriorities(
                new PropertyPriorityUpdateRequest(null, null), USER_KEY);

        assertThat(oldFirst.getPriorityRank()).isNull();
        assertThat(oldSecond.getPriorityRank()).isNull();
        assertThat(response.priorities()).isEmpty();
    }

    @Test
    void 다른_사용자의_매물은_우선순위로_지정할_수_없다() {
        User user = user(1L);
        when(anonymousUserService.findExisting(USER_KEY)).thenReturn(Optional.of(user));
        when(propertyRepository.findOwnedById(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propertyCommandService.updatePriorities(
                new PropertyPriorityUpdateRequest(99L, null), USER_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROPERTY_NOT_FOUND);

        verify(propertyRepository, never()).findAllByUserId(1L);
    }

    @Test
    void 같은_매물을_두_순위에_중복_지정할_수_없다() {
        assertThatThrownBy(() -> propertyCommandService.updatePriorities(
                new PropertyPriorityUpdateRequest(1L, 1L), USER_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROPERTY_DUPLICATE_PRIORITY);

        verify(anonymousUserService, never())
                .findExisting(org.mockito.ArgumentMatchers.any());
    }

    private Property property(Long id, Integer priorityRank) {
        Property property = new Property(
                "테스트 매물 " + id, 65_000L, 245_000L, 70_000L,
                priorityRank, LocalDateTime.now());
        org.springframework.test.util.ReflectionTestUtils.setField(property, "id", id);
        return property;
    }

    private User user(Long id) {
        User user = new User(USER_KEY);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
