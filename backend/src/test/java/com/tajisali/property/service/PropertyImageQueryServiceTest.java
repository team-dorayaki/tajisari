package com.tajisali.property.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.property.domain.Property;
import com.tajisali.property.domain.PropertyImage;
import com.tajisali.property.repository.PropertyImageRepository;
import com.tajisali.user.domain.User;
import com.tajisali.user.service.AnonymousUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyImageQueryServiceTest {

    private static final String USER_KEY = "00000000-0000-0000-0000-000000000001";

    @Mock
    private PropertyImageRepository propertyImageRepository;

    @Mock
    private AnonymousUserService anonymousUserService;

    @TempDir
    Path storageRoot;

    private PropertyImageQueryService propertyImageQueryService;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User(USER_KEY);
        ReflectionTestUtils.setField(user, "id", 1L);
        propertyImageQueryService = new PropertyImageQueryService(
                propertyImageRepository,
                anonymousUserService,
                new PropertyImageStorageService(storageRoot.toString()));
    }

    @Test
    void 본인_소유_PNG_이미지를_원본_내용과_ContentType으로_조회한다() throws Exception {
        byte[] content = {1, 2, 3};
        Path imagePath = storageRoot.resolve("properties/10/room.png");
        Files.createDirectories(imagePath.getParent());
        Files.write(imagePath, content);
        PropertyImage image = image("properties/10/room.png");
        when(anonymousUserService.findExisting(USER_KEY)).thenReturn(Optional.of(user));
        when(propertyImageRepository.findByIdAndProperty_User_Id(15L, 1L))
                .thenReturn(Optional.of(image));

        PropertyImageStorageService.StoredImageFile result =
                propertyImageQueryService.getImage(15L, USER_KEY);

        assertThat(result.contentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(result.resource().getInputStream().readAllBytes()).isEqualTo(content);
        verify(propertyImageRepository).findByIdAndProperty_User_Id(15L, 1L);
    }

    @Test
    void 쿠키가_없으면_이미지_존재여부와_관계없이_404를_반환한다() {
        when(anonymousUserService.findExisting(null)).thenReturn(Optional.empty());

        assertImageNotFound(() -> propertyImageQueryService.getImage(15L, null));

        verify(propertyImageRepository, never()).findByIdAndProperty_User_Id(15L, 1L);
    }

    @Test
    void 존재하지_않는_사용자도_404를_반환한다() {
        when(anonymousUserService.findExisting("unknown-user-key")).thenReturn(Optional.empty());

        assertImageNotFound(() -> propertyImageQueryService.getImage(15L, "unknown-user-key"));
    }

    @Test
    void 다른_사용자의_이미지는_404를_반환한다() {
        when(anonymousUserService.findExisting(USER_KEY)).thenReturn(Optional.of(user));
        when(propertyImageRepository.findByIdAndProperty_User_Id(15L, 1L))
                .thenReturn(Optional.empty());

        assertImageNotFound(() -> propertyImageQueryService.getImage(15L, USER_KEY));
    }

    @Test
    void 존재하지_않는_imageId는_404를_반환한다() {
        when(anonymousUserService.findExisting(USER_KEY)).thenReturn(Optional.of(user));
        when(propertyImageRepository.findByIdAndProperty_User_Id(15L, 1L))
                .thenReturn(Optional.empty());

        assertImageNotFound(() -> propertyImageQueryService.getImage(15L, USER_KEY));
    }

    @Test
    void DB에만_있고_파일이_없는_이미지는_404를_반환한다() {
        when(anonymousUserService.findExisting(USER_KEY)).thenReturn(Optional.of(user));
        when(propertyImageRepository.findByIdAndProperty_User_Id(15L, 1L))
                .thenReturn(Optional.of(image("properties/10/missing.png")));

        assertImageNotFound(() -> propertyImageQueryService.getImage(15L, USER_KEY));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../outside.png", "/tmp/outside.png"})
    void 저장소_밖을_가리키는_storageKey는_404를_반환한다(String storageKey) {
        when(anonymousUserService.findExisting(USER_KEY)).thenReturn(Optional.of(user));
        when(propertyImageRepository.findByIdAndProperty_User_Id(15L, 1L))
                .thenReturn(Optional.of(image(storageKey)));

        assertImageNotFound(() -> propertyImageQueryService.getImage(15L, USER_KEY));
    }

    private PropertyImage image(String storageKey) {
        Property property = new Property(
                "테스트 매물", 65_000L, 245_000L, 70_000L, null, LocalDateTime.now());
        property.assignOwner(user);
        PropertyImage image = new PropertyImage(
                property, storageKey, 0, "room.png", LocalDateTime.now());
        ReflectionTestUtils.setField(image, "id", 15L);
        return image;
    }

    private void assertImageNotFound(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROPERTY_IMAGE_NOT_FOUND);
    }
}
