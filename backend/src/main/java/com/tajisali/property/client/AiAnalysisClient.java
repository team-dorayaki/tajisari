package com.tajisali.property.client;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.property.dto.PropertyAnalysisResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.util.List;

@Slf4j
@Component
public class AiAnalysisClient {

    private final RestClient restClient;
    private final JsonMapper aiJsonMapper;

    public AiAnalysisClient(RestClient aiRestClient, JsonMapper jsonMapper) {
        this.restClient = aiRestClient;
        this.aiJsonMapper = jsonMapper.rebuild()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .build();
    }

    public PropertyAnalysisResponse analyzeImages(List<MultipartFile> files) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        for (MultipartFile file : files) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(file.getContentType()));
            Resource resource = file.getResource();
            parts.add("files", new HttpEntity<>(resource, headers));
        }

        return execute(() -> restClient.post()
                .uri("/api/v1/analysis/images")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts)
                .retrieve()
                .body(String.class));
    }

    public PropertyAnalysisResponse analyzeUrl(String url) {
        return execute(() -> restClient.post()
                .uri("/api/v1/analysis/url")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UrlRequest(url))
                .retrieve()
                .body(String.class));
    }

    private PropertyAnalysisResponse execute(AiRequest request) {
        try {
            return parseResponse(request.execute());
        } catch (RestClientResponseException exception) {
            log.warn("AI 분석 요청 실패 status={} code={}",
                    exception.getStatusCode().value(), readErrorCode(exception.getResponseBodyAsString()));
            ErrorCode errorCode = exception.getStatusCode().value() == 504
                    ? ErrorCode.PROPERTY_ANALYSIS_TIMEOUT
                    : ErrorCode.PROPERTY_ANALYSIS_FAILED;
            throw new BusinessException(errorCode, exception);
        } catch (ResourceAccessException exception) {
            ErrorCode errorCode = hasCause(exception, HttpTimeoutException.class)
                    ? ErrorCode.PROPERTY_ANALYSIS_TIMEOUT
                    : ErrorCode.PROPERTY_ANALYSIS_FAILED;
            log.warn("AI 서버 연결 실패 errorCode={}", errorCode.name());
            throw new BusinessException(errorCode, exception);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("AI 분석 응답 처리 실패", exception);
            throw new BusinessException(ErrorCode.PROPERTY_ANALYSIS_FAILED, exception);
        }
    }

    private PropertyAnalysisResponse parseResponse(String body) throws IOException {
        if (body == null || body.isBlank()) {
            throw new IOException("AI response body is empty");
        }

        JsonNode root = aiJsonMapper.readTree(body);
        JsonNode result = root.required("result");
        if (!result.isObject()) {
            throw new IOException("AI result is not an object");
        }

        PropertyAnalysisResponse.AnalysisMetadata metadata = aiJsonMapper.treeToValue(
                result.required("analysis_metadata"), PropertyAnalysisResponse.AnalysisMetadata.class);
        if (!"3.0".equals(metadata.schemaVersion())) {
            throw new IOException("Unsupported AI schema version");
        }

        return new PropertyAnalysisResponse(
                requiredText(root, "input_type"),
                requiredText(root, "model"),
                metadata,
                aiJsonMapper.treeToValue(result.required("property"), PropertyAnalysisResponse.Property.class),
                readList(result.required("property_cost_items"), PropertyAnalysisResponse.PropertyCostItem.class),
                aiJsonMapper.treeToValue(result.required("analysis_details"), PropertyAnalysisResponse.AnalysisDetails.class),
                result.deepCopy()
        );
    }

    private <T> List<T> readList(JsonNode node, Class<T> elementType) throws IOException {
        if (!node.isArray()) {
            throw new IOException("AI response field is not an array");
        }
        return aiJsonMapper.readerForListOf(elementType).readValue(node);
    }

    private String requiredText(JsonNode node, String field) throws IOException {
        JsonNode value = node.required(field);
        if (!value.isString() || value.asString().isBlank()) {
            throw new IOException("AI response field is not text: " + field);
        }
        return value.asString();
    }

    private String readErrorCode(String body) {
        try {
            JsonNode code = aiJsonMapper.readTree(body).path("error").path("code");
            return code.isString() ? code.asString() : "UNKNOWN";
        } catch (Exception exception) {
            return "UNKNOWN";
        }
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private record UrlRequest(String url) {
    }

    @FunctionalInterface
    private interface AiRequest {
        String execute();
    }
}
