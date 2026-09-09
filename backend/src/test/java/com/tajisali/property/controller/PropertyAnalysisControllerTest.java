package com.tajisali.property.controller;

import com.tajisali.common.config.JacksonConfig;
import com.tajisali.common.exception.GlobalExceptionHandler;
import com.tajisali.property.dto.PropertyAnalysisResponse;
import com.tajisali.property.service.PropertyAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PropertyAnalysisController.class)
@Import({JacksonConfig.class, GlobalExceptionHandler.class})
class PropertyAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PropertyAnalysisService propertyAnalysisService;

    @Test
    void 이미지_요청은_파일을_Service에_전달하고_공통_응답을_반환한다() throws Exception {
        MockMultipartFile image = image("property.png");
        when(propertyAnalysisService.analyzeImages(anyList())).thenReturn(response("images"));

        mockMvc.perform(multipart("/api/property-analyses/images").file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.inputType").value("images"))
                .andExpect(jsonPath("$.data.analysisMetadata.schemaVersion").value("3.0"))
                .andExpect(jsonPath("$.data.property.keyMoney").value(0))
                .andExpect(jsonPath("$.data.property.deposit").value((Object) null))
                .andExpect(jsonPath("$.data.rawResult.property.key_money").value(0))
                .andExpect(jsonPath("$.error").value((Object) null));

        verify(propertyAnalysisService).analyzeImages(anyList());
    }

    @Test
    void 이미지_세_장은_정상_범위로_Service에_전달한다() throws Exception {
        when(propertyAnalysisService.analyzeImages(anyList())).thenReturn(response("images"));

        mockMvc.perform(multipart("/api/property-analyses/images")
                        .file(image("1.png"))
                        .file(image("2.png"))
                        .file(image("3.png")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(propertyAnalysisService).analyzeImages(argThat(files -> files.size() == 3));
    }

    @Test
    void 이미지가_없으면_Service를_호출하지_않는다() throws Exception {
        mockMvc.perform(multipart("/api/property-analyses/images"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));

        verifyNoInteractions(propertyAnalysisService);
    }

    @Test
    void 빈_이미지나_네_장이면_Service를_호출하지_않는다() throws Exception {
        mockMvc.perform(multipart("/api/property-analyses/images")
                        .file(new MockMultipartFile("files", "empty.png", "image/png", new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));

        mockMvc.perform(multipart("/api/property-analyses/images")
                        .file(image("1.png"))
                        .file(image("2.png"))
                        .file(image("3.png"))
                        .file(image("4.png")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));

        verifyNoInteractions(propertyAnalysisService);
    }

    @Test
    void 지원하지_않는_이미지_MIME이면_Service를_호출하지_않는다() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile(
                "files", "property.txt", MediaType.TEXT_PLAIN_VALUE, new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/property-analyses/images").file(textFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));

        verifyNoInteractions(propertyAnalysisService);
    }

    @Test
    void URL_요청은_URL을_Service에_전달하고_공통_응답을_반환한다() throws Exception {
        when(propertyAnalysisService.analyzeUrl("https://suumo.jp/chintai/example"))
                .thenReturn(response("url"));

        mockMvc.perform(post("/api/property-analyses/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://suumo.jp/chintai/example\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.inputType").value("url"));

        verify(propertyAnalysisService).analyzeUrl("https://suumo.jp/chintai/example");
    }

    @Test
    void 비어_있거나_유효하지_않은_URL이면_Service를_호출하지_않는다() throws Exception {
        for (String body : List.of(
                "{}",
                "{\"url\":null}",
                "{\"url\":\" \"}",
                "{\"url\":\"not-a-url\"}",
                "{\"url\":\"ftp://example.com/property\"}"
        )) {
            mockMvc.perform(post("/api/property-analyses/url")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));
        }

        verifyNoInteractions(propertyAnalysisService);
    }

    private MockMultipartFile image(String filename) {
        return new MockMultipartFile("files", filename, "image/png", new byte[]{1, 2, 3});
    }

    private PropertyAnalysisResponse response(String inputType) {
        var rawResult = tools.jackson.databind.json.JsonMapper.builder().build().createObjectNode();
        rawResult.putObject("property").put("key_money", 0);

        return new PropertyAnalysisResponse(
                inputType,
                "gemini-3.5-flash-lite",
                new PropertyAnalysisResponse.AnalysisMetadata(
                        "3.0", PropertyAnalysisResponse.SourceType.IMAGE, 1),
                new PropertyAnalysisResponse.Property(
                        PropertyAnalysisResponse.SourceSite.SUUMO, null, "매물", "東京都", "北区",
                        null, null, null, 65_000L, 5_000L, null, 0L, null, null, null),
                List.of(),
                new PropertyAnalysisResponse.AnalysisDetails(
                        List.of(), List.of(), List.of(), List.of(), List.of(),
                        new PropertyAnalysisResponse.Validation(
                                List.of(), List.of(), List.of(),
                                new PropertyAnalysisResponse.ValidationChecks(
                                        true, true, true, true, true, true, true, true, true))),
                rawResult
        );
    }
}
