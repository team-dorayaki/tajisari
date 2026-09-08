package com.tajisali.common.response;

import com.tajisali.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void 성공_응답은_데이터와_null_오류를_포함한다() {
        var json = mapper.valueToTree(ApiResponse.success(new TestData(1L)));

        assertThat(json.size()).isEqualTo(3);
        assertThat(json.path("success").asBoolean()).isTrue();
        assertThat(json.path("data").path("id").asLong()).isEqualTo(1L);
        assertThat(json.has("error")).isTrue();
        assertThat(json.path("error").isNull()).isTrue();
    }

    @Test
    void 실패_응답은_null_데이터와_오류_코드_메시지를_포함한다() {
        var json = mapper.valueToTree(ApiResponse.failure(ErrorCode.COMMON_INVALID_INPUT));

        assertThat(json.size()).isEqualTo(3);
        assertThat(json.path("success").asBoolean()).isFalse();
        assertThat(json.has("data")).isTrue();
        assertThat(json.path("data").isNull()).isTrue();
        assertThat(json.path("error").size()).isEqualTo(2);
        assertThat(json.path("error").path("code").asString()).isEqualTo("COMMON_INVALID_INPUT");
        assertThat(json.path("error").path("message").asString()).isEqualTo("입력값을 확인해 주세요.");
    }

    public record TestData(Long id) {
    }
}
