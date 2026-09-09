package com.tajisali.home.controller;

import com.tajisali.common.config.JacksonConfig;
import com.tajisali.common.exception.GlobalExceptionHandler;
import com.tajisali.home.domain.HomeProgressStep;
import com.tajisali.home.service.HomeProgressService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HomeProgressController.class)
@Import({JacksonConfig.class, GlobalExceptionHandler.class})
class HomeProgressControllerTest {

    private static final String ANONYMOUS_USER_COOKIE = "tajisari_anonymous_user";
    private static final String USER_KEY = "00000000-0000-0000-0000-000000000901";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HomeProgressService homeProgressService;

    @Test
    void Cookie가_없으면_200과_STEP_1을_반환하고_Cookie를_발급하지_않는다() throws Exception {
        when(homeProgressService.getCurrentStep(isNull())).thenReturn(HomeProgressStep.STEP_1);

        mockMvc.perform(get("/api/me/home-progress"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(content().json("""
                        {"success":true,"data":{"currentStep":"STEP_1"},"error":null}
                        """));

        verify(homeProgressService).getCurrentStep(null);
    }

    @Test
    void Cookie의_userKey를_Service에_전달하고_Cookie를_발급하지_않는다() throws Exception {
        when(homeProgressService.getCurrentStep(USER_KEY)).thenReturn(HomeProgressStep.STEP_3);

        mockMvc.perform(get("/api/me/home-progress")
                        .cookie(new Cookie(ANONYMOUS_USER_COOKIE, USER_KEY)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(content().json("""
                        {"success":true,"data":{"currentStep":"STEP_3"},"error":null}
                        """));

        verify(homeProgressService).getCurrentStep(USER_KEY);
    }

    @ParameterizedTest
    @EnumSource(HomeProgressStep.class)
    void 모든_단계는_공통_ApiResponse의_문자열_currentStep으로_직렬화된다(HomeProgressStep step) throws Exception {
        when(homeProgressService.getCurrentStep(USER_KEY)).thenReturn(step);

        mockMvc.perform(get("/api/me/home-progress")
                        .cookie(new Cookie(ANONYMOUS_USER_COOKIE, USER_KEY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentStep").value(step.name()))
                .andExpect(jsonPath("$.data.currentStep").isString());
    }
}
