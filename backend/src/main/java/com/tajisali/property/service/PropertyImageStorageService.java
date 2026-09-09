package com.tajisali.property.service;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
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
    private static final Map<String, MediaType> CONTENT_TYPES_BY_EXTENSION = Map.of(
            "jpg", MediaType.IMAGE_JPEG,
            "jpeg", MediaType.IMAGE_JPEG,
            "png", MediaType.IMAGE_PNG,
            "webp", MediaType.parseMediaType("image/webp"),
            "bmp", MediaType.parseMediaType("image/bmp")
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

    public StoredImageFile load(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new BusinessException(ErrorCode.PROPERTY_IMAGE_NOT_FOUND);
        }
        try {
            Path storageKeyPath = Path.of(storageKey);
            if (storageKeyPath.isAbsolute()) {
                throw new BusinessException(ErrorCode.PROPERTY_IMAGE_NOT_FOUND);
            }

            Path imagePath = storageRoot.resolve(storageKeyPath).normalize();
            if (!imagePath.startsWith(storageRoot) || !Files.isRegularFile(imagePath)) {
                throw new BusinessException(ErrorCode.PROPERTY_IMAGE_NOT_FOUND);
            }

            Path realStorageRoot = storageRoot.toRealPath();
            Path realImagePath = imagePath.toRealPath();
            if (!realImagePath.startsWith(realStorageRoot)) {
                throw new BusinessException(ErrorCode.PROPERTY_IMAGE_NOT_FOUND);
            }

            MediaType contentType = contentTypeFor(realImagePath);
            if (contentType == null) {
                throw new BusinessException(ErrorCode.PROPERTY_IMAGE_NOT_FOUND);
            }
            return new StoredImageFile(new FileSystemResource(realImagePath), contentType);
        } catch (InvalidPathException | IOException | SecurityException exception) {
            throw new BusinessException(ErrorCode.PROPERTY_IMAGE_NOT_FOUND, exception);
        }
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

    private MediaType contentTypeFor(Path imagePath) {
        String filename = imagePath.getFileName().toString();
        int extensionIndex = filename.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == filename.length() - 1) {
            return null;
        }
        return CONTENT_TYPES_BY_EXTENSION.get(
                filename.substring(extensionIndex + 1).toLowerCase(Locale.ROOT));
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

    public record StoredImageFile(Resource resource, MediaType contentType) {
    }
}
