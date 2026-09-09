package com.tajisali.property.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class PropertyImageStorageService {

    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            MediaType.IMAGE_JPEG_VALUE, "jpg",
            MediaType.IMAGE_PNG_VALUE, "png",
            "image/webp", "webp",
            "image/bmp", "bmp"
    );

    private final Path storageRoot;

    public PropertyImageStorageService(
            @Value("${property.image.storage-root:uploads}") String storageRoot) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    public List<StoredImage> save(Long propertyId, List<MultipartFile> files) {
        if (propertyId == null || files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
        }

        List<String> extensions = files.stream()
                .map(this::extensionFor)
                .toList();
        Path propertyDirectory = propertyDirectory(propertyId);
        List<StoredImage> storedImages = new ArrayList<>();
        List<Path> createdPaths = new ArrayList<>();

        try {
            Files.createDirectories(propertyDirectory);
            for (int imageOrder = 0; imageOrder < files.size(); imageOrder++) {
                String storedFilename = UUID.randomUUID() + "." + extensions.get(imageOrder);
                Path destination = propertyDirectory.resolve(storedFilename).normalize();
                if (!destination.getParent().equals(propertyDirectory)) {
                    throw new IOException("Invalid property image storage path");
                }

                createdPaths.add(destination);
                try (InputStream inputStream = files.get(imageOrder).getInputStream()) {
                    Files.copy(inputStream, destination);
                }
                storedImages.add(new StoredImage(
                        "properties/" + propertyId + "/" + storedFilename,
                        files.get(imageOrder).getOriginalFilename(),
                        imageOrder,
                        destination));
            }
            return List.copyOf(storedImages);
        } catch (IOException exception) {
            cleanUp(createdPaths, propertyDirectory);
            throw new BusinessException(ErrorCode.PROPERTY_IMAGE_STORAGE_FAILED, exception);
        } catch (RuntimeException exception) {
            cleanUp(createdPaths, propertyDirectory);
            throw new BusinessException(ErrorCode.PROPERTY_IMAGE_STORAGE_FAILED, exception);
        }
    }

    public void cleanUp(List<StoredImage> storedImages) {
        if (storedImages.isEmpty()) {
            return;
        }
        Path propertyDirectory = storedImages.getFirst().path().getParent();
        cleanUp(storedImages.stream().map(StoredImage::path).toList(), propertyDirectory);
    }

    private String extensionFor(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
        }
        String extension = EXTENSIONS_BY_CONTENT_TYPE.get(file.getContentType());
        if (extension == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
        }
        return extension;
    }

    private Path propertyDirectory(Long propertyId) {
        return storageRoot.resolve("properties").resolve(propertyId.toString()).normalize();
    }

    private void cleanUp(List<Path> paths, Path propertyDirectory) {
        for (int index = paths.size() - 1; index >= 0; index--) {
            try {
                Files.deleteIfExists(paths.get(index));
            } catch (IOException exception) {
                log.warn("이미지 저장 실패 후 파일을 정리하지 못했습니다: {}", paths.get(index), exception);
            }
        }
        try {
            Files.deleteIfExists(propertyDirectory);
        } catch (IOException exception) {
            log.warn("이미지 저장 실패 후 매물 이미지 디렉터리를 정리하지 못했습니다: {}", propertyDirectory, exception);
        }
    }

    public record StoredImage(
            String storageKey,
            String originalFilename,
            int imageOrder,
            Path path
    ) {
    }
}
