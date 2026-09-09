package com.tajisali.property.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyImageStorageServiceTest {

    @TempDir
    Path storageRoot;

    @Test
    void 원본_파일명과_분리된상대_storageKey로_이미지를_저장하고_정리한다() throws Exception {
        PropertyImageStorageService storageService =
                new PropertyImageStorageService(storageRoot.toString());
        byte[] firstImage = {1, 2, 3};
        byte[] secondImage = {4, 5, 6};

        List<PropertyImageStorageService.StoredImage> storedImages = storageService.save(
                15L,
                List.of(
                        new MockMultipartFile(
                                "files", "../../first room.jpg", MediaType.IMAGE_JPEG_VALUE, firstImage),
                        new MockMultipartFile(
                                "files", "second.png", MediaType.IMAGE_PNG_VALUE, secondImage)));

        assertThat(storedImages)
                .extracting(image -> image.storageKey(), image -> image.imageOrder(),
                        image -> image.originalFilename())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                storedImages.getFirst().storageKey(), 0, "../../first room.jpg"),
                        org.assertj.core.groups.Tuple.tuple(
                                storedImages.get(1).storageKey(), 1, "second.png"));
        assertThat(storedImages.getFirst().storageKey())
                .matches("properties/15/[0-9a-f-]+\\.jpg");
        assertThat(storedImages.get(1).storageKey())
                .matches("properties/15/[0-9a-f-]+\\.png");
        assertThat(Files.readAllBytes(storedImages.getFirst().path())).isEqualTo(firstImage);
        assertThat(Files.readAllBytes(storedImages.get(1).path())).isEqualTo(secondImage);

        storageService.cleanUp(storedImages);

        assertThat(storageRoot.resolve("properties/15")).doesNotExist();
    }

    @Test
    void 두번째_파일_저장에_실패하면_같은_요청에서_저장한_파일을_정리한다() {
        PropertyImageStorageService storageService =
                new PropertyImageStorageService(storageRoot.toString());
        MockMultipartFile failingImage = new MockMultipartFile(
                "files", "second.png", MediaType.IMAGE_PNG_VALUE, new byte[]{4, 5, 6}) {
            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("read failure");
            }
        };

        assertThatThrownBy(() -> storageService.save(
                15L,
                List.of(
                        new MockMultipartFile(
                                "files", "first.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3}),
                        failingImage)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROPERTY_IMAGE_STORAGE_FAILED);

        assertThat(storageRoot.resolve("properties/15")).doesNotExist();
    }
}
