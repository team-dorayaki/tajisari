package com.tajisali.property.controller;

import com.tajisali.common.config.JacksonConfig;
import com.tajisali.common.exception.GlobalExceptionHandler;
import com.tajisali.property.dto.PropertyListResponse;
import com.tajisali.property.service.PropertyQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PropertyAnalysisController.class)
@Import({JacksonConfig.class, GlobalExceptionHandler.class})
class PropertyAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PropertyQueryService propertyQueryService;

    @Test
    void 매물_목록과_화면용_요약정보를_반환한다() throws Exception {
        var summary = new PropertyListResponse.PropertySummary(
                10L, 1L, "요코하마 스튜디오", 65_000L, 245_000L,
                new java.math.BigDecimal("3.2"), 1, "properties/10/thumbnail.jpg");
        when(propertyQueryService.getProperties())
                .thenReturn(new PropertyListResponse(1, List.of(summary)));

        mockMvc.perform(get("/api/property-analyses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.properties[0].propertyId").value(10))
                .andExpect(jsonPath("$.data.properties[0].analysisId").value(1))
                .andExpect(jsonPath("$.data.properties[0].propertyName").value("요코하마 스튜디오"))
                .andExpect(jsonPath("$.data.properties[0].rent").value(65_000))
                .andExpect(jsonPath("$.data.properties[0].initialCost").value(245_000))
                .andExpect(jsonPath("$.data.properties[0].livingMonths").value(3.2))
                .andExpect(jsonPath("$.data.properties[0].priorityRank").value(1))
                .andExpect(jsonPath("$.data.properties[0].thumbnailUrl")
                        .value("properties/10/thumbnail.jpg"))
                .andExpect(jsonPath("$.error").value((Object) null));

        verify(propertyQueryService).getProperties();
    }
}
