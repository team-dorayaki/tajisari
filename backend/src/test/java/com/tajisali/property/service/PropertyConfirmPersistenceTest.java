package com.tajisali.property.service;

import com.tajisali.property.domain.CostTiming;
import com.tajisali.property.domain.ObligationStatus;
import com.tajisali.property.dto.PropertyAnalysisResponse;
import com.tajisali.property.dto.PropertyConfirmRequest;
import com.tajisali.property.repository.PropertyAiAnalysisRepository;
import com.tajisali.property.repository.PropertyCostItemRepository;
import com.tajisali.property.repository.PropertyRepository;
import com.tajisali.user.repository.UserRepository;
import com.tajisali.user.service.AnonymousUserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        PropertyCommandService.class,
        PropertyCostCalculationService.class,
        AnonymousUserService.class
})
class PropertyConfirmPersistenceTest {

    @Autowired
    private PropertyCommandService propertyCommandService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyCostItemRepository propertyCostItemRepository;

    @Autowired
    private PropertyAiAnalysisRepository propertyAiAnalysisRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void URL_확인_결과의_매물_비용_AI원본을_한_사용자에게_저장한다() {
        var rawResult = tools.jackson.databind.json.JsonMapper.builder().build().createObjectNode();
        rawResult.putObject("property").put("key_money", 0);
        PropertyConfirmResult result = propertyCommandService.confirmUrl(
                new PropertyConfirmRequest(
                        PropertyAnalysisResponse.SourceType.URL,
                        "gemini-3.5-flash-lite",
                        new PropertyConfirmRequest.PropertyInfo(
                                PropertyAnalysisResponse.SourceSite.SUUMO,
                                "https://suumo.jp/chintai/confirmed-property",
                                "요코하마 스튜디오",
                                null, null, null, null, null,
                                65_000L, 5_000L, 100_000L, null, null, null, 999_999L),
                        List.of(
                                new PropertyConfirmRequest.PropertyCostItem(
                                        "계약사무수수료", "계약사무수수료", 20_000L, null,
                                        ObligationStatus.REQUIRED, true, CostTiming.INITIAL),
                                new PropertyConfirmRequest.PropertyCostItem(
                                        "선택 비용", "선택 비용", 0L, "0개월",
                                        ObligationStatus.OPTIONAL, false, CostTiming.INITIAL),
                                new PropertyConfirmRequest.PropertyCostItem(
                                        "월 관리 지원비", "월 관리 지원비", null, null,
                                        ObligationStatus.REQUIRED, true, CostTiming.MONTHLY)),
                        rawResult),
                null);
        entityManager.flush();
        entityManager.clear();

        var user = userRepository.findByUserKey(result.userKey()).orElseThrow();
        var property = propertyRepository.findByIdAndUserId(
                result.response().propertyId(), user.getId()).orElseThrow();
        var costItems = propertyCostItemRepository.findAll().stream()
                .filter(costItem -> costItem.getProperty().getId().equals(property.getId()))
                .toList();
        var analysis = propertyAiAnalysisRepository
                .findTopByPropertyIdOrderByIdDesc(property.getId()).orElseThrow();

        assertThat(property.getUser().getId()).isEqualTo(user.getId());
        assertThat(propertyRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()))
                .extracting(savedProperty -> savedProperty.getId())
                .contains(property.getId());
        assertThat(costItems).extracting(costItem -> costItem.getAmount())
                .containsExactlyInAnyOrder(20_000L, 0L, null);
        assertThat(property.getConfirmedInitialCost()).isEqualTo(120_000L);
        assertThat(property.getConfirmedMonthlyCost()).isEqualTo(70_000L);
        assertThat(property.getListedInitialCostTotal()).isEqualTo(999_999L);
        assertThat(analysis.getSourceType()).isEqualTo("URL");
        assertThat(analysis.getModelVersion()).isEqualTo("gemini-3.5-flash-lite");
        assertThat(analysis.getRawJson()).isEqualTo(rawResult.toString());
    }
}
