package com.tajisali.home.service;

import com.tajisali.home.domain.HomeProgressStep;
import com.tajisali.property.repository.PropertyRepository;
import com.tajisali.settlement.repository.SettlementPlanRepository;
import com.tajisali.user.domain.User;
import com.tajisali.user.service.AnonymousUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeProgressService {

    private final AnonymousUserService anonymousUserService;
    private final SettlementPlanRepository settlementPlanRepository;
    private final PropertyRepository propertyRepository;

    /**
     * 현재 익명 사용자의 홈 진행 단계를 계산한다.
     *
     * <p>조회 전용이므로 User를 새로 생성하지 않는다. 쿠키가 없거나 해당 userKey의 User가 없으면
     * 홈 첫 진입 상태로 보고 STEP_1을 반환한다.
     *
     * <p>단계가 결정되면 이후 조회를 수행하지 않는다.
     */
    @Transactional(readOnly = true)
    public HomeProgressStep getCurrentStep(String userKey) {
        User user = anonymousUserService.findExisting(userKey).orElse(null);
        if (user == null) {
            return HomeProgressStep.STEP_1;
        }

        Long userId = user.getId();
        if (!settlementPlanRepository.existsByUserId(userId)) {
            return HomeProgressStep.STEP_1;
        }
        if (!propertyRepository.existsByUserId(userId)) {
            return HomeProgressStep.STEP_2;
        }
        if (!user.isHasComparedProperties()) {
            return HomeProgressStep.STEP_3;
        }
        if (!propertyRepository.existsByUserIdAndPriorityRankIsNotNull(userId)) {
            return HomeProgressStep.STEP_4;
        }
        return HomeProgressStep.COMPLETED;
    }
}
