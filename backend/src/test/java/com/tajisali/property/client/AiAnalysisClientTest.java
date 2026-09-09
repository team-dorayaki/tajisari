package com.tajisali.property.client;

import com.tajisali.common.exception.BusinessException;
import com.tajisali.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class AiAnalysisClientTest {

    private MockRestServiceServer server;
    private AiAnalysisClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ai.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AiAnalysisClient(builder.build(), JsonMapper.builder().build());
    }

    @Test
    void 이미지는_AI_경로의_files_multipart로_전달된다() {
        server.expect(once(), requestTo("http://ai.test/api/v1/analysis/images"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andExpect(content().string(containsString("name=\"files\"")))
                .andExpect(content().string(containsString("property.png")))
                .andRespond(withSuccess(validResponse("images"), MediaType.APPLICATION_JSON));

        var response = client.analyzeImages(List.of(
                new MockMultipartFile("files", "property.png", "image/png", new byte[]{1, 2, 3})));

        assertThat(response.inputType()).isEqualTo("images");
        server.verify();
    }

    @Test
    void URL은_AI_경로의_JSON_body로_전달된다() {
        server.expect(once(), requestTo("http://ai.test/api/v1/analysis/url"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"url\":\"https://suumo.jp/chintai/example\"}"))
                .andRespond(withSuccess(validResponse("url"), MediaType.APPLICATION_JSON));

        var response = client.analyzeUrl("https://suumo.jp/chintai/example");

        assertThat(response.inputType()).isEqualTo("url");
        server.verify();
    }

    @Test
    void JSON_v3를_camelCase_DTO로_읽고_원본과_null_0을_보존한다() {
        server.expect(requestTo("http://ai.test/api/v1/analysis/url"))
                .andRespond(withSuccess(validResponse("url"), MediaType.APPLICATION_JSON));

        var response = client.analyzeUrl("https://suumo.jp/chintai/example");

        assertThat(response.modelVersion()).isEqualTo("gemini-3.5-flash-lite");
        assertThat(response.analysisMetadata().schemaVersion()).isEqualTo("3.0");
        assertThat(response.property().deposit()).isNull();
        assertThat(response.property().keyMoney()).isZero();
        assertThat(response.propertyCostItems()).singleElement().satisfies(item -> {
            assertThat(item.amount()).isNull();
            assertThat(item.obligationStatus()).isEqualTo(
                    com.tajisali.property.dto.PropertyAnalysisResponse.ObligationStatus.UNKNOWN);
        });
        assertThat(response.analysisDetails().fieldAnalysis()).singleElement().satisfies(item -> {
            assertThat(item.needsReview()).isTrue();
            assertThat(item.confidence()).isEqualByComparingTo("0.6");
            assertThat(item.evidence()).hasSize(1);
        });
        assertThat(response.rawResult().path("property").path("key_money").asLong()).isZero();
        assertThat(response.rawResult().path("property").path("deposit").isNull()).isTrue();
        assertThat(response.rawResult().has("property_cost_items")).isTrue();
        server.verify();
    }

    @Test
    void 대표적인_AI_실패는_공통_분석_실패로_변환된다() {
        server.expect(requestTo("http://ai.test/api/v1/analysis/url"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)
                        .body("{\"error\":{\"code\":\"GEMINI_RATE_LIMIT\",\"message\":\"internal\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.analyzeUrl("https://example.com"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.PROPERTY_ANALYSIS_FAILED));
        server.verify();
    }

    @Test
    void AI의_timeout_응답은_timeout_오류로_변환된다() {
        server.expect(requestTo("http://ai.test/api/v1/analysis/url"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.GATEWAY_TIMEOUT)
                        .body("{\"error\":{\"code\":\"GEMINI_TIMEOUT\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.analyzeUrl("https://example.com"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.PROPERTY_ANALYSIS_TIMEOUT));
        server.verify();
    }

    @Test
    void 지원하지_않는_AI_스키마는_공통_분석_실패로_변환된다() {
        server.expect(requestTo("http://ai.test/api/v1/analysis/url"))
                .andRespond(withSuccess(
                        validResponse("url").replace("\"3.0\"", "\"2.0\""),
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.analyzeUrl("https://example.com"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.PROPERTY_ANALYSIS_FAILED));
        server.verify();
    }

    @Test
    void AI_응답의_필수_구조가_없으면_공통_분석_실패로_변환된다() {
        server.expect(requestTo("http://ai.test/api/v1/analysis/url"))
                .andRespond(withSuccess(
                        "{\"input_type\":\"url\",\"model\":\"gemini-3.5-flash-lite\",\"result\":{}}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.analyzeUrl("https://example.com"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.PROPERTY_ANALYSIS_FAILED));
        server.verify();
    }

    @Test
    void AI_서버_연결_실패는_공통_분석_실패로_변환된다() {
        server.expect(requestTo("http://ai.test/api/v1/analysis/url"))
                .andRespond(request -> {
                    throw new ResourceAccessException("connection refused");
                });

        assertThatThrownBy(() -> client.analyzeUrl("https://example.com"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.PROPERTY_ANALYSIS_FAILED));
        server.verify();
    }

    private String validResponse(String inputType) {
        return """
                {
                  "input_type": "%s",
                  "model": "gemini-3.5-flash-lite",
                  "result": {
                    "analysis_metadata": {
                      "schema_version": "3.0",
                      "source_type": "URL",
                      "image_count": 0
                    },
                    "property": {
                      "source_site": "SUUMO",
                      "source_url": "https://suumo.jp/chintai/example",
                      "property_name": "サンプルハイツ 203",
                      "prefecture": "東京都",
                      "city": "北区",
                      "exclusive_area_m2": 18.0,
                      "nearest_station": "栄町駅",
                      "walk_minutes": 5,
                      "rent": 65000,
                      "management_fee": 5000,
                      "deposit": null,
                      "key_money": 0,
                      "available_from": null,
                      "contract_period_months": null,
                      "listed_initial_cost_total": null
                    },
                    "property_cost_items": [{
                      "raw_name": "保証委託料",
                      "display_name": "보증 위탁료",
                      "amount": null,
                      "raw_value": "賃料総額の50%%",
                      "obligation_status": "UNKNOWN",
                      "timing": "INITIAL"
                    }],
                    "analysis_details": {
                      "field_analysis": [{
                        "field": "property.deposit",
                        "raw_value": null,
                        "confidence": 0.6,
                        "needs_review": true,
                        "evidence": [{
                          "source_type": "URL",
                          "source_index": null,
                          "source_url": "https://suumo.jp/chintai/example",
                          "raw_text": "敷金記載なし"
                        }]
                      }],
                      "cost_item_analysis": [{
                        "cost_item_index": 0,
                        "scope": "LISTING_SPECIFIC",
                        "confidence": 0.8,
                        "needs_review": false,
                        "evidence": []
                      }],
                      "all_stations": [],
                      "additional_fields": [],
                      "reference_information": [],
                      "validation": {
                        "conflicts": [],
                        "warnings": [],
                        "unknown_fields": ["property.deposit"],
                        "checks": {
                          "evidence_only": true,
                          "amount_does_not_imply_required": true,
                          "zero_and_null_distinguished": true,
                          "duplicates_removed": true,
                          "conflicts_reviewed": true,
                          "fixed_costs_not_duplicated": true,
                          "listing_terms_preferred": true,
                          "amounts_match_raw_text": true,
                          "required_status_has_evidence": true
                        }
                      }
                    }
                  }
                }
                """.formatted(inputType);
    }
}
