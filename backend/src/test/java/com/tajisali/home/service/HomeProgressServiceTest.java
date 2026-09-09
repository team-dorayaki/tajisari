package com.tajisali.home.service;

import com.tajisali.home.domain.HomeProgressStep;
import com.tajisali.property.repository.PropertyRepository;
import com.tajisali.settlement.repository.SettlementPlanRepository;
import com.tajisali.user.domain.User;
import com.tajisali.user.service.AnonymousUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeProgressServiceTest {

    private static final String USER_KEY = "00000000-0000-0000-0000-000000000901";
    private static final Long USER_ID = 7L;

    @Mock
    AnonymousUserService anonymousUserService;
    @Mock
    SettlementPlanRepository settlementPlanRepository;
    @Mock
    PropertyRepository propertyRepository;

    private HomeProgressService homeProgressService;

    @BeforeEach
    void setUp() {
        homeProgressService = new HomeProgressService(
                anonymousUserService, settlementPlanRepository, propertyRepository);
    }

    @Test
    void 쿠키가_없으면_STEP_1이고_User를_생성하지_않는다() {
        when(anonymousUserService.findExisting(isNull())).thenReturn(Optional.empty());

        HomeProgressStep step = homeProgressService.getCurrentStep(null);

        assertThat(step).isEqualTo(HomeProgressStep.STEP_1);
        verify(anonymousUserService, never()).resolveOrCreate(any());
        verifyNoInteractions(settlementPlanRepository, propertyRepository);
    }

    @Test
    void 쿠키에_해당하는_User가_없으면_STEP_1이고_이후_조회를_하지_않는다() {
        when(anonymousUserService.findExisting(USER_KEY)).thenReturn(Optional.empty());

        HomeProgressStep step = homeProgressService.getCurrentStep(USER_KEY);

        assertThat(step).isEqualTo(HomeProgressStep.STEP_1);
        verify(anonymousUserService, never()).resolveOrCreate(any());
        verifyNoInteractions(settlementPlanRepository, propertyRepository);
    }

    @Test
    void 정착_계획이_없으면_STEP_1이고_Property를_조회하지_않는다() {
        givenUser(false);
        when(settlementPlanRepository.existsByUserId(USER_ID)).thenReturn(false);

        HomeProgressStep step = homeProgressService.getCurrentStep(USER_KEY);

        assertThat(step).isEqualTo(HomeProgressStep.STEP_1);
        verifyNoInteractions(propertyRepository);
    }

    @Test
    void 정착_계획은_있고_저장_매물이_없으면_STEP_2이고_우선순위를_조회하지_않는다() {
        givenUser(false);
        when(settlementPlanRepository.existsByUserId(USER_ID)).thenReturn(true);
        when(propertyRepository.existsByUserId(USER_ID)).thenReturn(false);

        HomeProgressStep step = homeProgressService.getCurrentStep(USER_KEY);

        assertThat(step).isEqualTo(HomeProgressStep.STEP_2);
        verify(propertyRepository).existsByUserId(USER_ID);
        verifyNoMoreInteractions(propertyRepository);
    }

    @Test
    void 비교_완료_이력이_없으면_STEP_3이고_우선순위를_조회하지_않는다() {
        givenUser(false);
        when(settlementPlanRepository.existsByUserId(USER_ID)).thenReturn(true);
        when(propertyRepository.existsByUserId(USER_ID)).thenReturn(true);

        HomeProgressStep step = homeProgressService.getCurrentStep(USER_KEY);

        assertThat(step).isEqualTo(HomeProgressStep.STEP_3);
        verify(propertyRepository).existsByUserId(USER_ID);
        verifyNoMoreInteractions(propertyRepository);
    }

    @Test
    void 우선순위_매물이_없으면_STEP_4이다() {
        givenUser(true);
        when(settlementPlanRepository.existsByUserId(USER_ID)).thenReturn(true);
        when(propertyRepository.existsByUserId(USER_ID)).thenReturn(true);
        when(propertyRepository.existsByUserIdAndPriorityRankIsNotNull(USER_ID)).thenReturn(false);

        HomeProgressStep step = homeProgressService.getCurrentStep(USER_KEY);

        assertThat(step).isEqualTo(HomeProgressStep.STEP_4);
    }

    @Test
    void 모든_조건을_충족하면_COMPLETED이다() {
        givenUser(true);
        when(settlementPlanRepository.existsByUserId(USER_ID)).thenReturn(true);
        when(propertyRepository.existsByUserId(USER_ID)).thenReturn(true);
        when(propertyRepository.existsByUserIdAndPriorityRankIsNotNull(USER_ID)).thenReturn(true);

        HomeProgressStep step = homeProgressService.getCurrentStep(USER_KEY);

        assertThat(step).isEqualTo(HomeProgressStep.COMPLETED);
    }

    @Test
    void 비교_이력이_없으면_우선순위_매물이_있어도_STEP_3이다() {
        givenUser(false);
        when(settlementPlanRepository.existsByUserId(USER_ID)).thenReturn(true);
        when(propertyRepository.existsByUserId(USER_ID)).thenReturn(true);

        HomeProgressStep step = homeProgressService.getCurrentStep(USER_KEY);

        assertThat(step).isEqualTo(HomeProgressStep.STEP_3);
        verify(propertyRepository, never()).existsByUserIdAndPriorityRankIsNotNull(any());
    }

    @Test
    void 모든_존재_여부_조회는_현재_User_기준으로_수행된다() {
        givenUser(true);
        when(settlementPlanRepository.existsByUserId(USER_ID)).thenReturn(true);
        when(propertyRepository.existsByUserId(USER_ID)).thenReturn(true);
        when(propertyRepository.existsByUserIdAndPriorityRankIsNotNull(USER_ID)).thenReturn(true);

        homeProgressService.getCurrentStep(USER_KEY);

        verify(anonymousUserService).findExisting(USER_KEY);
        verify(settlementPlanRepository).existsByUserId(USER_ID);
        verify(propertyRepository).existsByUserId(USER_ID);
        verify(propertyRepository).existsByUserIdAndPriorityRankIsNotNull(USER_ID);
        verifyNoMoreInteractions(anonymousUserService, settlementPlanRepository, propertyRepository);
    }

    private void givenUser(boolean hasComparedProperties) {
        User user = new User(USER_KEY);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        if (hasComparedProperties) {
            user.markPropertiesCompared();
        }
        when(anonymousUserService.findExisting(USER_KEY)).thenReturn(Optional.of(user));
    }
}
