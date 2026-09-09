package com.tajisali.property.controller;

import com.tajisali.common.config.JacksonConfig;
import com.tajisali.common.exception.GlobalExceptionHandler;
import com.tajisali.property.dto.PropertyConfirmResponse;
import com.tajisali.property.dto.PropertyListResponse;
import com.tajisali.property.dto.PropertyDetailResponse;
import com.tajisali.property.dto.PropertyPriorityUpdateRequest;
import com.tajisali.property.dto.PropertyPriorityUpdateResponse;
import com.tajisali.property.service.PropertyCommandService;
import com.tajisali.property.service.PropertyConfirmResult;
import com.tajisali.property.service.PropertyQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.math.BigDecimal;
import jakarta.servlet.http.Cookie;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PropertyController.class)
@Import({JacksonConfig.class, GlobalExceptionHandler.class})
class PropertyControllerTest {

    private static final String USER_KEY = "00000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PropertyQueryService propertyQueryService;

    @MockitoBean
    private PropertyCommandService propertyCommandService;

    @Test
    void URL_매물_확인_결과를_저장하고_propertyId를_반환한다() throws Exception {
        when(propertyCommandService.confirmUrl(any(), eq(USER_KEY)))
                .thenReturn(new PropertyConfirmResult(
                        new PropertyConfirmResponse(15L), USER_KEY));

        mockMvc.perform(post("/api/properties/confirm")
                        .cookie(new Cookie("tajisari_anonymous_user", USER_KEY))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest("https://suumo.jp/chintai/example", "URL")))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.propertyId").value(15))
                .andExpect(jsonPath("$.error").value((Object) null));

        verify(propertyCommandService).confirmUrl(any(), eq(USER_KEY));
    }

    @Test
    void 익명_사용자_쿠키가_없으면_저장_후_쿠키를_설정한다() throws Exception {
        String createdUserKey = "00000000-0000-0000-0000-000000000002";
        when(propertyCommandService.confirmUrl(any(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(new PropertyConfirmResult(
                        new PropertyConfirmResponse(15L), createdUserKey));

        mockMvc.perform(post("/api/properties/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest("https://suumo.jp/chintai/example", "URL")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString(
                        "tajisari_anonymous_user=" + createdUserKey)));
    }

    @Test
    void 필수_요청값이_없거나_URL_분석이_아니면_저장_Service를_호출하지_않는다() throws Exception {
        mockMvc.perform(post("/api/properties/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));

        mockMvc.perform(post("/api/properties/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest("https://suumo.jp/chintai/example", "IMAGE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));

        verifyNoInteractions(propertyCommandService);
    }

    @Test
    void 이미지_매물_확인_결과를_저장하고_기존_익명_사용자_쿠키를_재사용한다() throws Exception {
        when(propertyCommandService.confirmImages(any(), any(), eq(USER_KEY)))
                .thenReturn(new PropertyConfirmResult(
                        new PropertyConfirmResponse(16L), USER_KEY));

        mockMvc.perform(multipart("/api/properties/confirm/images")
                        .file(imageRequestPart())
                        .file(image("room-1.jpg", MediaType.IMAGE_JPEG_VALUE))
                        .cookie(new Cookie("tajisari_anonymous_user", USER_KEY)))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.propertyId").value(16));

        verify(propertyCommandService).confirmImages(
                org.mockito.ArgumentMatchers.argThat(request ->
                        request.sourceType().name().equals("IMAGE")
                                && request.property().sourceUrl() == null
                                && request.property().propertyName().equals("요코하마 스튜디오")),
                any(),
                eq(USER_KEY));
    }

    @Test
    void 이미지_세장은_저장할_수_있고_새_익명_사용자_쿠키를_설정한다() throws Exception {
        String createdUserKey = "00000000-0000-0000-0000-000000000002";
        when(propertyCommandService.confirmImages(any(), any(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(new PropertyConfirmResult(
                        new PropertyConfirmResponse(17L), createdUserKey));

        mockMvc.perform(multipart("/api/properties/confirm/images")
                        .file(imageRequestPart())
                        .file(image("room-1.png", MediaType.IMAGE_PNG_VALUE))
                        .file(image("room-2.webp", "image/webp"))
                        .file(image("room-3.bmp", "image/bmp")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString(
                        "tajisari_anonymous_user=" + createdUserKey)));
    }

    @Test
    void 이미지_저장_요청은_파일과_IMAGE_분석_결과만_허용한다() throws Exception {
        mockMvc.perform(multipart("/api/properties/confirm/images")
                        .file(imageRequestPart()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));

        mockMvc.perform(multipart("/api/properties/confirm/images")
                        .file(imageRequestPart())
                        .file(image("room-1.jpg", MediaType.IMAGE_JPEG_VALUE))
                        .file(image("room-2.jpg", MediaType.IMAGE_JPEG_VALUE))
                        .file(image("room-3.jpg", MediaType.IMAGE_JPEG_VALUE))
                        .file(image("room-4.jpg", MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));

        mockMvc.perform(multipart("/api/properties/confirm/images")
                        .file(imageRequestPart())
                        .file(new MockMultipartFile(
                                "files", "empty.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));

        mockMvc.perform(multipart("/api/properties/confirm/images")
                        .file(imageRequestPart())
                        .file(image("note.txt", MediaType.TEXT_PLAIN_VALUE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));

        mockMvc.perform(multipart("/api/properties/confirm/images")
                        .file(new MockMultipartFile(
                                "request", "", MediaType.APPLICATION_JSON_VALUE,
                                imageConfirmRequest("URL").getBytes()))
                        .file(image("room-1.jpg", MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));

        verifyNoInteractions(propertyCommandService);
    }

    @Test
    void 매물_목록과_화면용_요약정보를_반환한다() throws Exception {
        var summary = new PropertyListResponse.PropertySummary(
                10L, "요코하마 스튜디오", 65_000L, 245_000L,
                new java.math.BigDecimal("3.2"), 1, "properties/10/thumbnail.jpg");
        when(propertyQueryService.getProperties(USER_KEY))
                .thenReturn(new PropertyListResponse(1, List.of(summary)));

        mockMvc.perform(get("/api/properties")
                        .cookie(new Cookie("tajisari_anonymous_user", USER_KEY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.properties[0].propertyId").value(10))
                .andExpect(jsonPath("$.data.properties[0].propertyName").value("요코하마 스튜디오"))
                .andExpect(jsonPath("$.data.properties[0].rent").value(65_000))
                .andExpect(jsonPath("$.data.properties[0].initialCost").value(245_000))
                .andExpect(jsonPath("$.data.properties[0].livingMonths").value(3.2))
                .andExpect(jsonPath("$.data.properties[0].priorityRank").value(1))
                .andExpect(jsonPath("$.data.properties[0].thumbnailUrl")
                        .value("properties/10/thumbnail.jpg"))
                .andExpect(jsonPath("$.error").value((Object) null));

        verify(propertyQueryService).getProperties(USER_KEY);
    }

    @Test
    void 매물_ID로_매물_상세를_반환한다() throws Exception {
        var response = new PropertyDetailResponse(
                10L,
                7L,
                new PropertyDetailResponse.PropertyInfo(
                        "요코하마 스튜디오", "SUUMO", "https://suumo.jp/example",
                        "가나가와현", "요코하마시", new BigDecimal("25.40"),
                        "요코하마역", 8, null, 24, 1, 65_000L, 5_000L),
                List.of(new PropertyDetailResponse.PropertyImage(
                        1L, "https://cdn.example.com/room-1.jpg", 0)),
                new PropertyDetailResponse.CostAnalysis(
                        new PropertyDetailResponse.CostSummary(
                                245_000L, 227_000L, 70_000L,
                                227_000L, 18_000L,
                                65_000L, 180_000L, 33_000L),
                        new PropertyDetailResponse.CostGroups(
                                List.of(), List.of(), List.of(), List.of()),
                        List.of()),
                new PropertyDetailResponse.Simulation(
                        new PropertyDetailResponse.ExchangeRate(100, 860),
                        913_953L, 307_000L, true, 606_953L,
                        70_000L, 115_000L, 185_000L,
                        List.of(new PropertyDetailResponse.MonthlyBalance(1, 421_953L)),
                        new BigDecimal("3.2"), false, 12,
                        2_527_000L, 0L, 1_613_047L, 13_872_204L));
        when(propertyQueryService.getPropertyDetail(10L, USER_KEY)).thenReturn(response);

        mockMvc.perform(get("/api/properties/10")
                        .cookie(new Cookie("tajisari_anonymous_user", USER_KEY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.propertyId").value(10))
                .andExpect(jsonPath("$.data.images[0].imageUrl")
                        .value("https://cdn.example.com/room-1.jpg"))
                .andExpect(jsonPath("$.data.costAnalysis.summary.refundableAmount").value(65_000))
                .andExpect(jsonPath("$.data.costAnalysis.summary.minimumInitialCost").value(227_000))
                .andExpect(jsonPath("$.data.simulation.exchangeRate.krw").value(860))
                .andExpect(jsonPath("$.data.simulation.canMoveIn").value(true))
                .andExpect(jsonPath("$.data.simulation.monthlyBalances[0].month").value(1))
                .andExpect(jsonPath("$.data.simulation.livingMonths").value(3.2));

        verify(propertyQueryService).getPropertyDetail(10L, USER_KEY);
    }

    @Test
    void 매물_ID로_저장된_매물을_삭제한다() throws Exception {
        mockMvc.perform(delete("/api/properties/10")
                        .cookie(new Cookie("tajisari_anonymous_user", USER_KEY)))
                .andExpect(status().isNoContent());

        verify(propertyCommandService).deleteProperty(10L, USER_KEY);
    }

    @Test
    void 사용자의_매물_우선순위를_일괄_저장한다() throws Exception {
        var request = new PropertyPriorityUpdateRequest(10L, 20L);
        var response = new PropertyPriorityUpdateResponse(List.of(
                new PropertyPriorityUpdateResponse.Priority(10L, 1),
                new PropertyPriorityUpdateResponse.Priority(20L, 2)));
        when(propertyCommandService.updatePriorities(request, USER_KEY))
                .thenReturn(response);

        mockMvc.perform(put("/api/properties/priorities")
                        .cookie(new Cookie("tajisari_anonymous_user", USER_KEY))
                        .contentType("application/json")
                        .content("""
                                {
                                  "firstPriorityPropertyId": 10,
                                  "secondPriorityPropertyId": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.priorities[0].propertyId").value(10))
                .andExpect(jsonPath("$.data.priorities[0].priorityRank").value(1))
                .andExpect(jsonPath("$.data.priorities[1].propertyId").value(20))
                .andExpect(jsonPath("$.data.priorities[1].priorityRank").value(2))
                .andExpect(jsonPath("$.error").value((Object) null));

        verify(propertyCommandService).updatePriorities(request, USER_KEY);
    }

    private String confirmRequest(String sourceUrl, String sourceType) {
        return """
                {
                  "sourceType": "%s",
                  "modelVersion": "gemini-3.5-flash-lite",
                  "property": {
                    "sourceSite": "SUUMO",
                    "sourceUrl": "%s",
                    "propertyName": "요코하마 스튜디오"
                  },
                  "propertyCostItems": [],
                  "rawResult": {"property": {"key_money": 0}}
                }
                """.formatted(sourceType, sourceUrl);
    }

    private MockMultipartFile imageRequestPart() {
        return new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                imageConfirmRequest("IMAGE").getBytes());
    }

    private MockMultipartFile image(String filename, String contentType) {
        return new MockMultipartFile("files", filename, contentType, new byte[]{1, 2, 3});
    }

    private String imageConfirmRequest(String sourceType) {
        String sourceUrl = "URL".equals(sourceType)
                ? "\"sourceUrl\": \"https://suumo.jp/chintai/example\","
                : "";
        return """
                {
                  "sourceType": "%s",
                  "modelVersion": "gemini-3.5-flash-lite",
                  "property": {
                    "sourceSite": "SUUMO",
                    %s
                    "propertyName": "요코하마 스튜디오"
                  },
                  "propertyCostItems": [],
                  "rawResult": {"property": {"key_money": 0}}
                }
                """.formatted(sourceType, sourceUrl);
    }
}
