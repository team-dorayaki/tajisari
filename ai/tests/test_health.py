from types import SimpleNamespace

import pytest
from fastapi.testclient import TestClient

from app.core.config import settings
from app.core.errors import classify_gemini_error
from app.main import app
from app.schemas.analysis_output import OUTPUT_SCHEMA
from gemini_analyze_image import (
    apply_semantic_checks,
    infer_cost_timing,
    infer_obligation_status,
    validate_interaction_status,
    validate_url_context_result,
)


client = TestClient(app)


@pytest.mark.parametrize(
    ("text", "expected"),
    [
        ("화재보험 2년 ¥13,130 ~ ¥26,680", "UNKNOWN"),
        ("열쇠교환비 ¥16,500", "UNKNOWN"),
        ("월정액 주차장 ¥4,400~", "MONTHLY"),
        ("보증료 10,000엔 (2년차 이후, 매년)", "RENEWAL"),
        ("이용료의 100%~120% (계약 시)", "INITIAL"),
        ("更新料 ¥16,500", "RENEWAL"),
        ("インターネット利用料 月 ¥3,630", "MONTHLY"),
        ("환경유지비 ¥550/월", "MONTHLY"),
    ],
)
def test_rule_based_cost_timing_uses_direct_evidence_only(text: str, expected: str) -> None:
    assert infer_cost_timing(text, "INITIAL") == expected


@pytest.mark.parametrize(
    ("text", "expected"),
    [
        ("항균시공 (임의) ¥18,040~", "OPTIONAL"),
        ("가입 필수 보증료", "REQUIRED"),
        ("월정액 주차장 공실 있음 이용 가능", "UNKNOWN"),
        ("중개수수료 제로 불필요", "UNKNOWN"),
        ("保証会社利用必 初回1万円", "REQUIRED"),
    ],
)
def test_rule_based_obligation_uses_explicit_words(text: str, expected: str) -> None:
    assert infer_obligation_status(text) == expected


def test_health() -> None:
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}
    assert response.headers["X-Request-ID"]


def test_image_upload_openapi_uses_binary_files() -> None:
    schema = client.get("/openapi.json").json()
    request_schema = schema["paths"]["/api/property-analyses/images"]["post"]["requestBody"][
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
    request_schema = schema["paths"]["/api/property-analyses/url"]["post"]["requestBody"][
        "content"
    ]["application/json"]["schema"]
    schema_name = request_schema["$ref"].rsplit("/", 1)[-1]
    url_schema = schema["components"]["schemas"][schema_name]

    assert set(url_schema["properties"]) == {"url"}
    assert url_schema["required"] == ["url"]


def test_invalid_url_returns_standard_error() -> None:
    response = client.post("/api/property-analyses/url", json={"url": "not-a-url"})

    assert response.status_code == 422
    assert response.json()["error"]["code"] == "INVALID_URL"
    assert response.json()["error"]["request_id"] == response.headers["X-Request-ID"]
    assert response.json()["error"]["retryable"] is False


def test_invalid_image_type_returns_standard_error() -> None:
    response = client.post(
        "/api/property-analyses/images",
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
    response = client.post("/api/property-analyses/images", files=files)

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


def test_analysis_output_schema_v31_matches_database_fields() -> None:
    properties = OUTPUT_SCHEMA["properties"]
    fixed_fields = properties["property"]["properties"]

    assert set(properties) == {
        "analysis_metadata",
        "property",
        "property_cost_items",
        "analysis_details",
    }
    assert properties["analysis_metadata"]["properties"]["schema_version"]["enum"] == [
        "3.1"
    ]
    assert "cost_candidates" in properties["analysis_details"]["properties"]
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
    assert data["analysis_details"]["cost_item_analysis"][0]["needs_review"] is True
    assert data["property_cost_items"][1]["amount"] is None
    assert data["property_cost_items"][1]["obligation_status"] == "REQUIRED"
    checks = data["analysis_details"]["validation"]["checks"]
    assert checks["amounts_match_raw_text"] is False
    assert checks["required_status_has_evidence"] is False


def test_semantic_checks_exclude_cost_not_applied_to_listing() -> None:
    data = {
        "property": {},
        "property_cost_items": [
            {
                "raw_name": "サポート費",
                "display_name": "서포트비",
                "amount": 15000,
                "raw_value": "会社一般案内 15,000円",
                "obligation_status": "UNKNOWN",
                "timing": "INITIAL",
            }
        ],
        "analysis_details": {
            "field_analysis": [],
            "cost_item_analysis": [
                {
                    "cost_item_index": 0,
                    "scope": "LISTING_SPECIFIC",
                    "confidence": 0.5,
                    "needs_review": True,
                    "evidence": [{"raw_text": "会社一般案内 15,000円"}],
                }
            ],
            "cost_candidates": [
                {
                    "raw_text": "会社一般案内 15,000円",
                    "cost_name": "サポート費",
                    "applicability_condition": None,
                    "applies_to_listing": "UNKNOWN",
                    "timing": "INITIAL",
                    "destination": "PROPERTY_COST_ITEM",
                    "target_index": 0,
                    "evidence": [{"raw_text": "会社一般案内 15,000円"}],
                }
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

    assert data["property_cost_items"] == []
    candidate = data["analysis_details"]["cost_candidates"][0]
    assert candidate["destination"] == "REFERENCE_INFORMATION"
    assert candidate["target_index"] is None
    assert data["analysis_details"]["validation"]["checks"][
        "cost_candidates_classified"
    ] is True


def test_semantic_checks_restore_applicable_excluded_cost_candidate() -> None:
    data = {
        "property": {},
        "property_cost_items": [],
        "analysis_details": {
            "field_analysis": [],
            "cost_item_analysis": [],
            "cost_candidates": [
                {
                    "raw_text": "火災保険 2年 13,130円～26,680円",
                    "cost_name": "火災保険",
                    "applicability_condition": None,
                    "applies_to_listing": "YES",
                    "timing": "CONDITIONAL",
                    "destination": "EXCLUDED",
                    "target_index": None,
                    "evidence": [{"raw_text": "火災保険 2年 13,130円～26,680円"}],
                }
            ],
            "all_stations": [],
            "reference_information": [],
            "validation": {
                "warnings": [], "unknown_fields": [], "conflicts": [], "checks": {}
            },
        },
    }

    apply_semantic_checks(data)

    assert len(data["property_cost_items"]) == 1
    assert data["property_cost_items"][0]["amount"] is None
    assert data["property_cost_items"][0]["obligation_status"] == "UNKNOWN"
    assert data["property_cost_items"][0]["timing"] == "UNKNOWN"
    assert data["analysis_details"]["cost_item_analysis"][0]["needs_review"] is True
    candidate = data["analysis_details"]["cost_candidates"][0]
    assert candidate["destination"] == "PROPERTY_COST_ITEM"
    assert candidate["target_index"] == 0
    assert data["analysis_details"]["validation"]["checks"][
        "cost_candidates_classified"
    ] is True


def test_semantic_checks_split_guarantee_cost_by_timing() -> None:
    data = {
        "property": {},
        "property_cost_items": [],
        "analysis_details": {
            "field_analysis": [],
            "cost_item_analysis": [],
            "cost_candidates": [
                {
                    "raw_text": "利用料の100%～120%(契約時)/10,000円(2年차 이후, 매년)",
                    "cost_name": "보증 위탁료",
                    "applicability_condition": "개인 계약만",
                    "applies_to_listing": "CONDITIONAL",
                    "timing": "CONDITIONAL",
                    "destination": "EXCLUDED",
                    "target_index": None,
                    "evidence": [
                        {
                            "raw_text": "利用料の100%～120%(契約時)/10,000円(2年차 이후, 매년)"
                        }
                    ],
                }
            ],
            "all_stations": [],
            "reference_information": [],
            "validation": {
                "warnings": [], "unknown_fields": [], "conflicts": [], "checks": {}
            },
        },
    }

    apply_semantic_checks(data)

    assert [item["timing"] for item in data["property_cost_items"]] == [
        "INITIAL", "RENEWAL"
    ]
    assert [item["amount"] for item in data["property_cost_items"]] == [None, 10000]
    assert len(data["analysis_details"]["cost_candidates"]) == 2


def test_semantic_checks_enforce_rules_and_remove_no_cost_rows() -> None:
    raw_values = [
        "항균시공 (임의) ¥18,040~¥23,760",
        "중개수수료 제로 불필요",
        "보증금 차감 불필요",
    ]
    names = ["항균시공", "중개수수료", "보증금 차감"]
    data = {
        "property": {},
        "property_cost_items": [
            {
                "raw_name": name,
                "display_name": name,
                "amount": None,
                "raw_value": raw_value,
                "obligation_status": "UNKNOWN",
                "timing": "INITIAL",
            }
            for name, raw_value in zip(names, raw_values)
        ],
        "analysis_details": {
            "field_analysis": [],
            "cost_item_analysis": [
                {
                    "cost_item_index": index,
                    "scope": "LISTING_SPECIFIC",
                    "confidence": 0.9,
                    "needs_review": False,
                    "evidence": [{"raw_text": raw_value}],
                }
                for index, raw_value in enumerate(raw_values)
            ],
            "cost_candidates": [
                {
                    "raw_text": raw_value,
                    "cost_name": name,
                    "display_name": name,
                    "applicability_condition": None,
                    "applies_to_listing": "YES",
                    "timing": "INITIAL",
                    "destination": "PROPERTY_COST_ITEM",
                    "target_index": index,
                    "evidence": [],
                }
                for index, (name, raw_value) in enumerate(zip(names, raw_values))
            ],
            "all_stations": [],
            "additional_fields": [],
            "reference_information": [],
            "validation": {
                "warnings": [], "unknown_fields": [], "conflicts": [], "checks": {}
            },
        },
    }

    apply_semantic_checks(data)

    assert len(data["property_cost_items"]) == 1
    assert data["property_cost_items"][0]["raw_name"] == "항균시공"
    assert data["property_cost_items"][0]["obligation_status"] == "OPTIONAL"
    assert data["property_cost_items"][0]["timing"] == "UNKNOWN"
    candidates = data["analysis_details"]["cost_candidates"]
    assert candidates[1]["destination"] == "EXCLUDED"
    assert candidates[2]["destination"] == "EXCLUDED"
    assert data["analysis_details"]["validation"]["checks"][
        "cost_candidates_classified"
    ] is True


def test_semantic_checks_reconcile_direct_deposit_candidate() -> None:
    evidence = [{"source_type": "IMAGE", "source_index": 1,
                 "source_url": None, "raw_text": "敷金/礼金 8.66万円 / -"}]
    data = {
        "property": {"deposit": 0, "key_money": 0},
        "property_cost_items": [],
        "analysis_details": {
            "field_analysis": [{
                "field": "property.deposit",
                "raw_value": "-",
                "confidence": 1,
                "needs_review": False,
                "evidence": evidence,
            }],
            "cost_item_analysis": [],
            "cost_candidates": [{
                "raw_text": "敷金 8.66万円",
                "cost_name": "敷金",
                "display_name": "보증금",
                "applicability_condition": None,
                "applies_to_listing": "YES",
                "timing": "UNKNOWN",
                "destination": "PROPERTY",
                "target_index": None,
                "evidence": evidence,
            }],
            "all_stations": [],
            "additional_fields": [],
            "reference_information": [],
            "validation": {
                "warnings": [], "unknown_fields": [], "conflicts": [], "checks": {}
            },
        },
    }

    apply_semantic_checks(data)

    assert data["property"]["deposit"] == 86600
    meta = data["analysis_details"]["field_analysis"][0]
    assert meta["raw_value"] == "敷金 8.66万円"
    assert meta["needs_review"] is False
    conflict = data["analysis_details"]["validation"]["conflicts"][0]
    assert conflict["field"] == "property.deposit"
    assert conflict["resolution"] == "RESOLVED"
    assert conflict["resolved_value"] == "86600"


def test_semantic_checks_do_not_apply_conditional_pet_deposit() -> None:
    data = {
        "property": {"deposit": 0},
        "property_cost_items": [],
        "analysis_details": {
            "field_analysis": [],
            "cost_item_analysis": [],
            "cost_candidates": [{
                "raw_text": "ペット飼育時 敷金86,600円追加",
                "cost_name": "敷金",
                "display_name": "반려동물 사육 시 추가 보증금",
                "applicability_condition": "ペット飼育時",
                "applies_to_listing": "CONDITIONAL",
                "timing": "CONDITIONAL",
                "destination": "PROPERTY",
                "target_index": None,
                "evidence": [],
            }],
            "all_stations": [],
            "additional_fields": [],
            "reference_information": [],
            "validation": {
                "warnings": [], "unknown_fields": [], "conflicts": [], "checks": {}
            },
        },
    }

    apply_semantic_checks(data)

    assert data["property"]["deposit"] == 0
    assert data["analysis_details"]["validation"]["conflicts"] == []


def test_semantic_checks_leave_different_positive_fixed_cost_unresolved() -> None:
    data = {
        "property": {"deposit": 50000},
        "property_cost_items": [],
        "analysis_details": {
            "field_analysis": [{
                "field": "property.deposit",
                "raw_value": "敷金 50,000円",
                "confidence": 1,
                "needs_review": False,
                "evidence": [{"raw_text": "敷金 50,000円"}],
            }],
            "cost_item_analysis": [],
            "cost_candidates": [{
                "raw_text": "敷金 86,600円",
                "cost_name": "敷金",
                "display_name": "보증금",
                "applicability_condition": None,
                "applies_to_listing": "YES",
                "timing": "UNKNOWN",
                "destination": "PROPERTY",
                "target_index": None,
                "evidence": [{"raw_text": "敷金 86,600円"}],
            }],
            "all_stations": [],
            "additional_fields": [],
            "reference_information": [],
            "validation": {
                "warnings": [], "unknown_fields": [], "conflicts": [], "checks": {}
            },
        },
    }

    apply_semantic_checks(data)

    assert data["property"]["deposit"] is None
    conflict = data["analysis_details"]["validation"]["conflicts"][0]
    assert conflict["resolution"] == "UNKNOWN"
    assert "property.deposit" in data["analysis_details"]["validation"]["unknown_fields"]


def test_semantic_checks_reject_month_based_key_money_integer() -> None:
    data = {
        "property": {"key_money": 0},
        "property_cost_items": [],
        "analysis_details": {
            "field_analysis": [{
                "field": "property.key_money",
                "raw_value": "0",
                "confidence": 1,
                "needs_review": False,
                "evidence": [{"raw_text": "礼金 1ヶ月"}],
            }],
            "cost_item_analysis": [],
            "cost_candidates": [{
                "raw_text": "礼金 1ヶ月",
                "cost_name": "礼金",
                "display_name": "사례금",
                "applicability_condition": None,
                "applies_to_listing": "YES",
                "timing": "UNKNOWN",
                "destination": "PROPERTY",
                "target_index": None,
                "evidence": [{"raw_text": "礼金 1ヶ月"}],
            }],
            "all_stations": [],
            "additional_fields": [],
            "reference_information": [],
            "validation": {
                "warnings": [], "unknown_fields": [], "conflicts": [], "checks": {}
            },
        },
    }

    apply_semantic_checks(data)

    assert data["property"]["key_money"] is None
    assert "property.key_money" in data["analysis_details"]["validation"]["unknown_fields"]
    conflict = data["analysis_details"]["validation"]["conflicts"][0]
    assert conflict["resolution"] == "UNKNOWN"
    assert "礼金 1ヶ月" in conflict["values"]


def test_semantic_checks_split_combined_deposit_and_key_money_candidate() -> None:
    evidence = [{"raw_text": "敷金/礼金 8.66万円 / -"}]
    data = {
        "property": {"deposit": None, "key_money": None},
        "property_cost_items": [],
        "analysis_details": {
            "field_analysis": [
                {
                    "field": field,
                    "raw_value": None,
                    "confidence": 1,
                    "needs_review": False,
                    "evidence": evidence,
                }
                for field in ("property.deposit", "property.key_money")
            ],
            "cost_item_analysis": [],
            "cost_candidates": [{
                "raw_text": "8.66万円 / -",
                "cost_name": "敷金/礼金",
                "display_name": "보증금/사례금",
                "applicability_condition": None,
                "applies_to_listing": "YES",
                "timing": "UNKNOWN",
                "destination": "PROPERTY",
                "target_index": None,
                "evidence": evidence,
            }],
            "all_stations": [],
            "additional_fields": [],
            "reference_information": [],
            "validation": {
                "warnings": [], "unknown_fields": [], "conflicts": [], "checks": {}
            },
        },
    }

    apply_semantic_checks(data)

    assert data["property"]["deposit"] == 86600
    assert data["property"]["key_money"] == 0
    assert "property.deposit" not in data["analysis_details"]["validation"]["unknown_fields"]
    assert "property.key_money" not in data["analysis_details"]["validation"]["unknown_fields"]
    assert {
        conflict["field"]
        for conflict in data["analysis_details"]["validation"]["conflicts"]
    } == {"property.deposit", "property.key_money"}


def test_semantic_checks_keep_monthly_guarantee_containing_rent_word() -> None:
    raw_text = "保証会社利用必 毎月家賃総額1％"
    data = {
        "property": {},
        "property_cost_items": [{
            "raw_name": raw_text,
            "display_name": "보증회사 월 보증료",
            "amount": None,
            "raw_value": "毎月家賃総額1％",
            "obligation_status": "REQUIRED",
            "timing": "MONTHLY",
        }],
        "analysis_details": {
            "field_analysis": [],
            "cost_item_analysis": [{
                "cost_item_index": 0,
                "scope": "LISTING_SPECIFIC",
                "confidence": 1,
                "needs_review": False,
                "evidence": [{"raw_text": raw_text}],
            }],
            "cost_candidates": [{
                "raw_text": "毎月家賃総額1％",
                "cost_name": "保証会社月額保証料",
                "display_name": "보증회사 월 보증료",
                "applicability_condition": None,
                "applies_to_listing": "YES",
                "timing": "MONTHLY",
                "destination": "PROPERTY_COST_ITEM",
                "target_index": 0,
                "evidence": [{"raw_text": raw_text}],
            }],
            "all_stations": [],
            "additional_fields": [],
            "reference_information": [],
            "validation": {
                "warnings": [], "unknown_fields": [], "conflicts": [], "checks": {}
            },
        },
    }

    apply_semantic_checks(data)

    assert len(data["property_cost_items"]) == 1
    assert data["property_cost_items"][0]["timing"] == "MONTHLY"
    assert data["property_cost_items"][0]["obligation_status"] == "REQUIRED"


def test_semantic_checks_exclude_japanese_unnecessary_cost() -> None:
    raw_text = "敷引金 不必要"
    data = {
        "property": {},
        "property_cost_items": [{
            "raw_name": "敷引金",
            "display_name": "보증금 차감",
            "amount": None,
            "raw_value": raw_text,
            "obligation_status": "UNKNOWN",
            "timing": "UNKNOWN",
        }],
        "analysis_details": {
            "field_analysis": [],
            "cost_item_analysis": [{
                "cost_item_index": 0,
                "scope": "LISTING_SPECIFIC",
                "confidence": 1,
                "needs_review": False,
                "evidence": [{"raw_text": raw_text}],
            }],
            "cost_candidates": [{
                "raw_text": raw_text,
                "cost_name": "敷引金",
                "display_name": "보증금 차감",
                "applicability_condition": None,
                "applies_to_listing": "YES",
                "timing": "UNKNOWN",
                "destination": "PROPERTY_COST_ITEM",
                "target_index": 0,
                "evidence": [{"raw_text": raw_text}],
            }],
            "all_stations": [],
            "additional_fields": [],
            "reference_information": [],
            "validation": {
                "warnings": [], "unknown_fields": [], "conflicts": [], "checks": {}
            },
        },
    }

    apply_semantic_checks(data)

    assert data["property_cost_items"] == []
    assert data["analysis_details"]["cost_candidates"][0]["destination"] == "EXCLUDED"
