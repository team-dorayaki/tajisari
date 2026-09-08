from types import SimpleNamespace

import pytest
from fastapi.testclient import TestClient

from app.core.config import settings
from app.core.errors import classify_gemini_error
from app.main import app
from app.schemas.analysis_output import OUTPUT_SCHEMA
from gemini_analyze_image import (
    apply_semantic_checks,
    validate_interaction_status,
    validate_url_context_result,
)


client = TestClient(app)


def test_health() -> None:
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}
    assert response.headers["X-Request-ID"]


def test_image_upload_openapi_uses_binary_files() -> None:
    schema = client.get("/openapi.json").json()
    request_schema = schema["paths"]["/api/v1/analysis/images"]["post"]["requestBody"][
        "content"
    ]["multipart/form-data"]["schema"]
    schema_name = request_schema["$ref"].rsplit("/", 1)[-1]
    file_schema = schema["components"]["schemas"][schema_name]["properties"]["files"]
    properties = schema["components"]["schemas"][schema_name]["properties"]

    assert file_schema["type"] == "array"
    assert file_schema["items"] == {"type": "string", "format": "binary"}
    assert set(properties) == {"files"}


def test_url_analysis_request_only_accepts_url() -> None:
    schema = client.get("/openapi.json").json()
    request_schema = schema["paths"]["/api/v1/analysis/url"]["post"]["requestBody"][
        "content"
    ]["application/json"]["schema"]
    schema_name = request_schema["$ref"].rsplit("/", 1)[-1]
    url_schema = schema["components"]["schemas"][schema_name]

    assert set(url_schema["properties"]) == {"url"}
    assert url_schema["required"] == ["url"]


def test_invalid_url_returns_standard_error() -> None:
    response = client.post("/api/v1/analysis/url", json={"url": "not-a-url"})

    assert response.status_code == 422
    assert response.json()["error"]["code"] == "INVALID_URL"
    assert response.json()["error"]["request_id"] == response.headers["X-Request-ID"]
    assert response.json()["error"]["retryable"] is False


def test_invalid_image_type_returns_standard_error() -> None:
    response = client.post(
        "/api/v1/analysis/images",
        files=[("files", ("property.txt", b"not an image", "text/plain"))],
    )

    assert response.status_code == 400
    assert response.json()["error"]["code"] == "INVALID_IMAGE_TYPE"
    assert response.json()["error"]["request_id"] == response.headers["X-Request-ID"]


def test_too_many_images_returns_standard_error() -> None:
    files = [
        ("files", (f"property-{index}.png", b"image", "image/png"))
        for index in range(settings.max_image_count + 1)
    ]
    response = client.post("/api/v1/analysis/images", files=files)

    assert response.status_code == 413
    assert response.json()["error"]["code"] == "TOO_MANY_IMAGES"
    assert response.json()["error"]["retryable"] is False


@pytest.mark.parametrize("status", ["incomplete", "budget_exceeded"])
def test_incomplete_interaction_is_detected_as_truncated(status: str) -> None:
    with pytest.raises(RuntimeError, match="GEMINI_RESPONSE_TRUNCATED"):
        validate_interaction_status(SimpleNamespace(status=status))


@pytest.mark.parametrize(
    ("status", "expected_marker", "expected_code"),
    [
        ("error", "URL_CONTEXT_FAILED", "URL_CONTEXT_FAILED"),
        ("paywall", "URL_CONTEXT_PAYWALL", "URL_CONTEXT_PAYWALL"),
        ("unsafe", "URL_CONTEXT_UNSAFE", "URL_CONTEXT_UNSAFE"),
    ],
)
def test_url_context_failure_statuses_are_classified(
    status: str, expected_marker: str, expected_code: str
) -> None:
    interaction = SimpleNamespace(
        steps=[
            SimpleNamespace(
                type="url_context_result",
                is_error=status == "error",
                result=[SimpleNamespace(status=status)],
            )
        ]
    )

    with pytest.raises(RuntimeError, match=expected_marker) as caught:
        validate_url_context_result(interaction)

    app_error = classify_gemini_error(caught.value, "url")
    assert app_error.code == expected_code


def test_analysis_output_schema_v3_matches_database_fields() -> None:
    properties = OUTPUT_SCHEMA["properties"]
    fixed_fields = properties["property"]["properties"]

    assert set(properties) == {
        "analysis_metadata",
        "property",
        "property_cost_items",
        "analysis_details",
    }
    assert properties["analysis_metadata"]["properties"]["schema_version"]["enum"] == [
        "3.0"
    ]
    assert set(fixed_fields) == {
        "source_site",
        "source_url",
        "property_name",
        "prefecture",
        "city",
        "exclusive_area_m2",
        "nearest_station",
        "walk_minutes",
        "rent",
        "management_fee",
        "deposit",
        "key_money",
        "available_from",
        "contract_period_months",
        "listed_initial_cost_total",
    }
    assert "common_service_fee" not in fixed_fields


def test_property_cost_item_matches_database_insert_fields() -> None:
    cost_item = OUTPUT_SCHEMA["properties"]["property_cost_items"]["items"]

    assert set(cost_item["properties"]) == {
        "raw_name",
        "display_name",
        "amount",
        "raw_value",
        "obligation_status",
        "timing",
    }


def test_semantic_checks_remove_model_calculations() -> None:
    data = {
        "property": {
            "source_site": "GTN_BEST_ESTATE",
            "source_url": None,
            "property_name": "테스트 매물",
            "prefecture": "도쿄도",
            "city": "기타구",
            "exclusive_area_m2": 18.0,
            "nearest_station": None,
            "walk_minutes": None,
            "rent": 65000,
            "management_fee": 8000,
            "deposit": 65000,
            "key_money": 0,
            "available_from": None,
            "contract_period_months": 24,
            "listed_initial_cost_total": None,
        },
        "property_cost_items": [
            {
                "raw_name": "保証委託料",
                "display_name": "초기 보증위탁료",
                "amount": 35000,
                "raw_value": "総賃料の50%",
                "obligation_status": "REQUIRED",
                "timing": "INITIAL",
            },
            {
                "raw_name": "家賃",
                "display_name": "월세",
                "amount": 65000,
                "raw_value": "65,000円",
                "obligation_status": "REQUIRED",
                "timing": "MONTHLY",
            },
        ],
        "analysis_details": {
            "field_analysis": [
                {
                    "field": "property.deposit",
                    "raw_value": "1ヶ月",
                    "confidence": 0.9,
                    "needs_review": False,
                    "evidence": [{"raw_text": "敷金 1ヶ月"}],
                },
                {
                    "field": "property.management_fee",
                    "raw_value": "管理費 5,000円 / 共益費 3,000円",
                    "confidence": 0.9,
                    "needs_review": False,
                    "evidence": [{"raw_text": "管理費 5,000円 / 共益費 3,000円"}],
                },
                {
                    "field": "property.contract_period_months",
                    "raw_value": "2年",
                    "confidence": 0.9,
                    "needs_review": False,
                    "evidence": [{"raw_text": "契約期間 2年"}],
                },
            ],
            "cost_item_analysis": [
                {
                    "cost_item_index": 0,
                    "scope": "LISTING_SPECIFIC",
                    "confidence": 0.8,
                    "needs_review": False,
                    "evidence": [{"raw_text": "初回保証料 総賃料の50%"}],
                },
                {
                    "cost_item_index": 1,
                    "scope": "LISTING_SPECIFIC",
                    "confidence": 1.0,
                    "needs_review": False,
                    "evidence": [{"raw_text": "家賃 65,000円"}],
                },
            ],
            "all_stations": [
                {
                    "line_name": "京浜東北線",
                    "station_name": "王子駅",
                    "walk_minutes": 8,
                    "evidence": [],
                },
                {
                    "line_name": "都電荒川線",
                    "station_name": "栄町駅",
                    "walk_minutes": 5,
                    "evidence": [],
                },
            ],
            "additional_fields": [],
            "reference_information": [],
            "validation": {
                "warnings": [],
                "unknown_fields": [],
                "conflicts": [],
                "checks": {},
            },
        },
    }

    apply_semantic_checks(data)

    assert data["property"]["nearest_station"] == "栄町駅"
    assert data["property"]["walk_minutes"] == 5
    assert data["property"]["deposit"] is None
    assert data["property"]["management_fee"] is None
    assert data["property"]["contract_period_months"] is None
    assert len(data["property_cost_items"]) == 1
    assert data["property_cost_items"][0]["amount"] is None
    assert data["analysis_details"]["cost_item_analysis"][0]["needs_review"] is True


def test_semantic_checks_normalize_paths_and_reject_unsupported_cost_values() -> None:
    data = {
        "property": {
            "rent": 80000,
            "management_fee": None,
            "deposit": 80000,
            "key_money": 80000,
        },
        "property_cost_items": [
            {
                "raw_name": "初期費用",
                "display_name": "초기 비용",
                "amount": 30000,
                "raw_value": "30,000円",
                "obligation_status": "REQUIRED",
                "timing": "INITIAL",
            },
            {
                "raw_name": "解約事務手数料",
                "display_name": "해약 사무 수수료",
                "amount": 1500,
                "raw_value": "15,000円（税別）",
                "obligation_status": "REQUIRED",
                "timing": "MOVE_OUT",
            },
        ],
        "analysis_details": {
            "field_analysis": [
                {
                    "field": "rent",
                    "raw_value": "80,000円",
                    "confidence": 1.0,
                    "needs_review": False,
                    "evidence": [{"raw_text": "賃料80,000円"}],
                }
            ],
            "cost_item_analysis": [
                {
                    "cost_item_index": 0,
                    "scope": "LISTING_SPECIFIC",
                    "confidence": 1.0,
                    "needs_review": False,
                    "evidence": [{"raw_text": "初期費用30,000円"}],
                },
                {
                    "cost_item_index": 1,
                    "scope": "LISTING_SPECIFIC",
                    "confidence": 1.0,
                    "needs_review": False,
                    "evidence": [
                        {"raw_text": "退去時に解約事務手数料15,000円が発生します"}
                    ],
                },
            ],
            "all_stations": [],
            "reference_information": [],
            "validation": {
                "warnings": [],
                "unknown_fields": [],
                "conflicts": [],
                "checks": {},
            },
        },
    }

    apply_semantic_checks(data)

    assert data["analysis_details"]["field_analysis"][0]["field"] == "property.rent"
    assert data["property_cost_items"][0]["obligation_status"] == "UNKNOWN"
    assert data["property_cost_items"][1]["amount"] is None
    assert data["property_cost_items"][1]["obligation_status"] == "REQUIRED"
    checks = data["analysis_details"]["validation"]["checks"]
    assert checks["amounts_match_raw_text"] is False
    assert checks["required_status_has_evidence"] is False
