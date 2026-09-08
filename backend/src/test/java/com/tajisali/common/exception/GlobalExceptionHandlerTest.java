package com.tajisali.common.exception;

import com.tajisali.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class GlobalExceptionHandlerTest {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @ParameterizedTest
    @CsvSource({
            "COMMON_INVALID_REQUEST, 400",
            "COMMON_INVALID_INPUT, 400",
            "SETTLEMENT_PLAN_NOT_FOUND, 404",
            "SETTLEMENT_PLAN_RESERVE_EXCEEDS_FUNDS, 400",
            "SETTLEMENT_PLAN_INVALID_COST_TYPE, 400",
            "SETTLEMENT_PLAN_DUPLICATE_COST_TYPE, 400",
            "SETTLEMENT_PLAN_COST_TOTAL_OVERFLOW, 400",
            "COMMON_INTERNAL_ERROR, 500"
    })
    void 비즈니스_예외는_계약의_상태와_코드로_반환된다(ErrorCode errorCode, int status) throws Exception {
        var result = mockMvc.perform(get("/test/business").param("code", errorCode.name())).andReturn();

        assertError(result, status, errorCode.name());
    }

    @Test
    void 필수값이_누락되면_입력_검증_오류를_반환한다() throws Exception {
        var result = mockMvc.perform(post("/test/input").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andReturn();

        assertError(result, 400, "COMMON_INVALID_INPUT");
        assertThat(result.getResolvedException())
                .isInstanceOf(org.springframework.web.bind.MethodArgumentNotValidException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{",
            "{\"date\":\"not-a-date\",\"choice\":\"ACCEPTED\"}",
            "{\"date\":\"2026-02-30\",\"choice\":\"ACCEPTED\"}",
            "{\"date\":\"2026-10-15\",\"choice\":\"UNKNOWN\"}"
    })
    void 잘못된_JSON_날짜_Enum은_요청_형식_오류를_반환한다(String body) throws Exception {
        var result = mockMvc.perform(post("/test/input").contentType(MediaType.APPLICATION_JSON)
                .content(body)).andReturn();

        assertError(result, 400, "COMMON_INVALID_REQUEST");
    }

    @Test
    void 정상_날짜와_Enum은_역직렬화된다() throws Exception {
        var result = mockMvc.perform(post("/test/input").contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2026-10-15\",\"choice\":\"ACCEPTED\"}")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(mapper.readTree(result.getResponse().getContentAsString()).path("success").asBoolean()).isTrue();
    }

    @Test
    void 메서드_입력_검증_실패는_입력_오류를_반환한다() throws Exception {
        var result = mockMvc.perform(get("/test/parameter").param("count", "0")).andReturn();

        assertError(result, 400, "COMMON_INVALID_INPUT");
    }

    @Test
    void 내부_예외는_500으로_반환하고_내부_정보를_노출하지_않는다() throws Exception {
        var result = mockMvc.perform(get("/test/unexpected")).andReturn();

        assertError(result, 500, "COMMON_INTERNAL_ERROR");
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("SELECT", "private_table", "IllegalStateException", "TestController", "stackTrace");
    }

    private void assertError(MvcResult result, int status, String code) throws Exception {
        var json = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(result.getResponse().getStatus()).isEqualTo(status);
        assertThat(json.size()).isEqualTo(3);
        assertThat(json.path("success").asBoolean()).isFalse();
        assertThat(json.has("data")).isTrue();
        assertThat(json.path("data").isNull()).isTrue();
        assertThat(json.path("error").size()).isEqualTo(2);
        assertThat(json.path("error").path("code").asString()).isEqualTo(code);
        assertThat(json.path("error").path("message").asString())
                .isEqualTo(ErrorCode.valueOf(code).getMessage());
    }

    @Test
    void 쿼리_타입_변환에_실패하면_400을_유지한다() throws Exception {
        var result = mockMvc.perform(get("/test/parameter").param("count", "invalid")).andReturn();

        assertError(result, 400, "COMMON_INVALID_REQUEST");
        assertThat(result.getResolvedException())
                .isInstanceOf(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class);
    }

    @Test
    void 필수_쿼리_파라미터가_없으면_400을_유지한다() throws Exception {
        var result = mockMvc.perform(get("/test/parameter")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResolvedException())
                .isInstanceOf(org.springframework.web.bind.MissingServletRequestParameterException.class);
    }

    @Test
    void 지원하지_않는_메서드는_405와_Allow_헤더를_유지한다() throws Exception {
        var result = mockMvc.perform(get("/test/input")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(405);
        assertThat(result.getResponse().getHeader("Allow")).isEqualTo("POST");
        assertThat(result.getResolvedException())
                .isInstanceOf(org.springframework.web.HttpRequestMethodNotSupportedException.class);
    }

    @Test
    void 지원하지_않는_ContentType은_415를_유지한다() throws Exception {
        var result = mockMvc.perform(post("/test/input").contentType(MediaType.TEXT_PLAIN)
                .content("input")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(415);
        assertThat(result.getResolvedException())
                .isInstanceOf(org.springframework.web.HttpMediaTypeNotSupportedException.class);
    }

    @RestController
    static class TestController {

        @GetMapping("/test/business")
        ApiResponse<Void> business(@RequestParam ErrorCode code) {
            throw new BusinessException(code);
        }

        @PostMapping("/test/input")
        ApiResponse<TestInput> input(@Valid @RequestBody TestInput input) {
            return ApiResponse.success(input);
        }

        @GetMapping("/test/parameter")
        ApiResponse<Integer> parameter(@RequestParam @Min(1) int count) {
            return ApiResponse.success(count);
        }

        @GetMapping("/test/unexpected")
        ApiResponse<Void> unexpected() {
            throw new IllegalStateException("SELECT * FROM private_table");
        }
    }

    public record TestInput(@NotNull LocalDate date, @NotNull TestChoice choice) {
    }

    public enum TestChoice {
        ACCEPTED
    }
}
