package com.tajisali.property.service;

import com.tajisali.property.domain.CostTiming;
import com.tajisali.property.domain.ObligationStatus;
import com.tajisali.property.dto.PropertyAnalysisResponse;
import com.tajisali.property.dto.PropertyConfirmRequest;
import com.tajisali.property.repository.PropertyAiAnalysisRepository;
import com.tajisali.property.repository.PropertyCostItemRepository;
import com.tajisali.property.repository.PropertyImageRepository;
import com.tajisali.property.repository.PropertyRepository;
import com.tajisali.user.repository.UserRepository;
import com.tajisali.user.service.AnonymousUserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PropertyCostCalculationService.class, AnonymousUserService.class})
class PropertyImageConfirmPersistenceTest {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyAiAnalysisRepository propertyAiAnalysisRepository;

    @Autowired
    private PropertyImageRepository propertyImageRepository;

    @Autowired
    private PropertyCostItemRepository propertyCostItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnonymousUserService anonymousUserService;

    @Autowired
    private PropertyCostCalculationService propertyCostCalculationService;

    @Autowired
    private EntityManager entityManager;

    @TempDir
    Path storageRoot;

    @Test
    void 이미지_확인_결과는_파일과_이미지메타데이터_비용_AI원본을_함께_저장한다() throws Exception {
        PropertyCommandService propertyCommandService = new PropertyCommandService(
                propertyRepository,
                propertyAiAnalysisRepository,
                propertyImageRepository,
                propertyCostItemRepository,
                anonymousUserService,
                propertyCostCalculationService,
                new PropertyImageStorageService(storageRoot.toString()));
        var rawResult = tools.jackson.databind.json.JsonMapper.builder().build().createObjectNode();
        rawResult.putObject("property").put("source", "image-persistence-test");
        byte[] firstImage = {1, 2, 3};
        byte[] secondImage = {4, 5, 6};

        PropertyConfirmResult result = propertyCommandService.confirmImages(
                new PropertyConfirmRequest(
                        PropertyAnalysisResponse.SourceType.IMAGE,
                        "gemini-test",
                        new PropertyConfirmRequest.PropertyInfo(
                                PropertyAnalysisResponse.SourceSite.SUUMO,
                                null,
                                "이미지 검증 매물",
                                null, null, null, null, null,
                                65_000L, 5_000L, 100_000L, null, null, null, 999_999L),
                        List.of(new PropertyConfirmRequest.PropertyCostItem(
                                "계약사무수수료", "계약사무수수료", 20_000L, null,
                                ObligationStatus.REQUIRED, true, CostTiming.INITIAL)),
                        rawResult),
                List.of(
                        new MockMultipartFile(
                                "files", "first room.jpg", MediaType.IMAGE_JPEG_VALUE, firstImage),
                        new MockMultipartFile(
                                "files", "second room.png", MediaType.IMAGE_PNG_VALUE, secondImage)),
                null);
        entityManager.flush();
        entityManager.clear();

        var user = userRepository.findByUserKey(result.userKey()).orElseThrow();
        var property = propertyRepository.findByIdAndUserId(
                result.response().propertyId(), user.getId()).orElseThrow();
        var images = propertyImageRepository.findAll().stream()
                .filter(image -> image.getProperty().getId().equals(property.getId()))
                .toList();
        var analysis = propertyAiAnalysisRepository
                .findTopByPropertyIdOrderByIdDesc(property.getId()).orElseThrow();

        assertThat(property.getSourceUrl()).isNull();
        assertThat(property.getConfirmedInitialCost()).isEqualTo(120_000L);
        assertThat(property.getConfirmedMonthlyCost()).isEqualTo(70_000L);
        assertThat(images)
                .extracting(image -> image.getImageOrder(), image -> image.getOriginalFilename())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(0, "first room.jpg"),
                        org.assertj.core.groups.Tuple.tuple(1, "second room.png"));
        assertThat(images).allSatisfy(image -> {
            assertThat(image.getStorageKey()).startsWith("properties/" + property.getId() + "/");
            assertThat(Path.of(image.getStorageKey())).isRelative();
        });
        assertThat(Files.readAllBytes(storageRoot.resolve(images.getFirst().getStorageKey())))
                .isEqualTo(firstImage);
        assertThat(Files.readAllBytes(storageRoot.resolve(images.get(1).getStorageKey())))
                .isEqualTo(secondImage);
        assertThat(propertyImageRepository.findByIdAndProperty_User_Id(
                images.getFirst().getId(), user.getId())).isPresent();
        var otherUser = userRepository.save(new com.tajisali.user.domain.User(UUID.randomUUID().toString()));
        assertThat(propertyImageRepository.findByIdAndProperty_User_Id(
                images.getFirst().getId(), otherUser.getId())).isEmpty();
        assertThat(analysis.getSourceType()).isEqualTo("IMAGE");
        assertThat(tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(analysis.getRawJson())).isEqualTo(rawResult);
    }
}
