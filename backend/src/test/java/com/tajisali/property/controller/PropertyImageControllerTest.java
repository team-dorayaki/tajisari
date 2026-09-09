package com.tajisali.property.controller;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import com.tajisali.common.exception.GlobalExceptionHandler;
import com.tajisali.property.service.PropertyImageQueryService;
import com.tajisali.property.service.PropertyImageStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PropertyImageController.class)
@Import(GlobalExceptionHandler.class)
class PropertyImageControllerTest {

    private static final String USER_KEY = "00000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PropertyImageQueryService propertyImageQueryService;

    @Test
    void 본인_소유_PNG_이미지를_바이너리로_반환한다() throws Exception {
        byte[] image = {1, 2, 3};
        when(propertyImageQueryService.getImage(15L, USER_KEY))
                .thenReturn(new PropertyImageStorageService.StoredImageFile(
                        new ByteArrayResource(image), MediaType.IMAGE_PNG));

        mockMvc.perform(get("/api/property-images/15")
                        .cookie(new Cookie("tajisari_anonymous_user", USER_KEY)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(image));
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "image/webp", "image/bmp"})
    void 저장된_이미지의_MIME을_그대로_반환한다(String contentType) throws Exception {
        MediaType mediaType = MediaType.parseMediaType(contentType);
        when(propertyImageQueryService.getImage(15L, USER_KEY))
                .thenReturn(new PropertyImageStorageService.StoredImageFile(
                        new ByteArrayResource(new byte[]{1}), mediaType));

        mockMvc.perform(get("/api/property-images/15")
                        .cookie(new Cookie("tajisari_anonymous_user", USER_KEY)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(mediaType));
    }

    @Test
    void 쿠키가_없으면_404를_반환한다() throws Exception {
        when(propertyImageQueryService.getImage(eq(15L), org.mockito.ArgumentMatchers.isNull()))
                .thenThrow(new BusinessException(ErrorCode.PROPERTY_IMAGE_NOT_FOUND));

        mockMvc.perform(get("/api/property-images/15"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.error.code").value("PROPERTY_IMAGE_NOT_FOUND"));
    }
}
