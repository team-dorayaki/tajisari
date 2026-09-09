package com.tajisali.property.service;

import com.tajisali.property.client.AiAnalysisClient;
import com.tajisali.property.dto.PropertyAnalysisResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyAnalysisServiceTest {

    private final AiAnalysisClient aiAnalysisClient = mock(AiAnalysisClient.class);
    private final PropertyAnalysisService service = new PropertyAnalysisService(aiAnalysisClient);

    @Test
    void 이미지_분석은_AI_Client에_위임한다() {
        List<MultipartFile> files = List.of(
                new MockMultipartFile("files", "property.png", "image/png", new byte[]{1}));
        PropertyAnalysisResponse response = mock(PropertyAnalysisResponse.class);
        when(aiAnalysisClient.analyzeImages(files)).thenReturn(response);

        assertThat(service.analyzeImages(files)).isSameAs(response);
        verify(aiAnalysisClient).analyzeImages(files);
    }

    @Test
    void URL_분석은_AI_Client에_위임한다() {
        String url = "https://suumo.jp/chintai/example";
        PropertyAnalysisResponse response = mock(PropertyAnalysisResponse.class);
        when(aiAnalysisClient.analyzeUrl(url)).thenReturn(response);

        assertThat(service.analyzeUrl(url)).isSameAs(response);
        verify(aiAnalysisClient).analyzeUrl(url);
    }
}
