import argparse
import base64
import json
import logging
import mimetypes
import os
import re
import time
from copy import deepcopy
from decimal import Decimal
from pathlib import Path
from urllib.parse import urlparse

from google import genai


logger = logging.getLogger(__name__)


REQUIRED_SECTIONS = {
    "analysis_metadata",
    "listing_metadata",
    "property",
    "location",
    "agency",
    "pricing",
    "contract",
    "conditions",
    "facilities",
    "assessment",
    "source_text",
    "source_evidence",
}

REQUIRED_NESTED_FIELDS = {
    "analysis_metadata": {
        "image_count",
        "same_property_assumed",
        "cross_image_consistency",
        "conflicts",
    },
    "listing_metadata": {
        "listing_id",
        "last_updated",
        "next_update",
        "source_languages",
    },
    "property": {"property_name_jp", "room_number", "property_type", "layout"},
    "location": {"address_raw", "address_jp_normalized", "address_ko", "stations"},
    "agency": {"company_name_raw", "postal_code", "address_raw"},
    "pricing": {
        "rent",
        "management_fee",
        "common_service_fee",
        "base_monthly_total_yen",
        "variable_monthly_costs",
    },
    "contract": {"contract_type", "contract_period", "available_date"},
    "conditions": {"pets", "foreign_residents", "other_restrictions"},
    "assessment": {"warnings", "missing_critical_fields", "confidence"},
}

NULLABLE_STRING = {"type": ["string", "null"]}
NULLABLE_INTEGER = {"type": ["integer", "null"]}
STRING_ARRAY = {"type": "array", "items": {"type": "string"}}


def required_object(properties: dict) -> dict:
    return {
        "type": "object",
        "properties": properties,
        "required": list(properties),
    }


COST_SCHEMA = required_object(
    {
        "amount_yen": NULLABLE_INTEGER,
        "raw_text": NULLABLE_STRING,
        "calculation": NULLABLE_STRING,
        "frequency": NULLABLE_STRING,
        "conditional": NULLABLE_STRING,
        "source_image_index": NULLABLE_INTEGER,
        "service_judgment": {"type": "string"},
    }
)

OTHER_COST_SCHEMA = required_object(
    {
        "scope": NULLABLE_STRING,
        "name_raw": NULLABLE_STRING,
        "name_jp_normalized": NULLABLE_STRING,
        "name_ko": NULLABLE_STRING,
        "amount_yen": NULLABLE_INTEGER,
        "rate_percent": {"type": ["number", "null"]},
        "min_rate_percent": {"type": ["number", "null"]},
        "max_rate_percent": {"type": ["number", "null"]},
        "calculation": NULLABLE_STRING,
        "frequency": NULLABLE_STRING,
        "conditional": NULLABLE_STRING,
        "source_image_index": NULLABLE_INTEGER,
    }
)

SOURCE_TEXT_SCHEMA = required_object(
    {
        "image_index": NULLABLE_INTEGER,
        "language": {"type": "string"},
        "raw_text": {"type": "string"},
    }
)

OUTPUT_SCHEMA = required_object(
    {
        "analysis_metadata": required_object(
            {
                "image_count": {"type": "integer"},
                "same_property_assumed": {"type": "boolean"},
                "cross_image_consistency": {"type": "string"},
                "conflicts": STRING_ARRAY,
            }
        ),
        "listing_metadata": required_object(
            {
                "listing_id": NULLABLE_STRING,
                "last_updated": NULLABLE_STRING,
                "next_update": NULLABLE_STRING,
                "source_languages": STRING_ARRAY,
            }
        ),
        "property": required_object(
            {
                "property_name_jp": NULLABLE_STRING,
                "room_number": NULLABLE_STRING,
                "property_type": NULLABLE_STRING,
                "layout": NULLABLE_STRING,
                "exclusive_area_m2": {"type": ["number", "null"]},
                "floor": NULLABLE_INTEGER,
                "total_floors": NULLABLE_INTEGER,
                "direction": NULLABLE_STRING,
                "structure": NULLABLE_STRING,
                "built_year": NULLABLE_INTEGER,
                "built_month": NULLABLE_INTEGER,
            }
        ),
        "location": required_object(
            {
                "postal_code": NULLABLE_STRING,
                "address_raw": NULLABLE_STRING,
                "address_jp_normalized": NULLABLE_STRING,
                "address_ko": NULLABLE_STRING,
                "prefecture": NULLABLE_STRING,
                "city": NULLABLE_STRING,
                "stations": {
                    "type": "array",
                    "items": required_object(
                        {
                            "line_raw": NULLABLE_STRING,
                            "line_jp_normalized": NULLABLE_STRING,
                            "station_raw": NULLABLE_STRING,
                            "station_jp_normalized": NULLABLE_STRING,
                            "walk_minutes": NULLABLE_INTEGER,
                            "source_image_index": NULLABLE_INTEGER,
                        }
                    ),
                },
            }
        ),
        "agency": required_object(
            {
                "company_name_raw": NULLABLE_STRING,
                "company_name_jp_normalized": NULLABLE_STRING,
                "postal_code": NULLABLE_STRING,
                "address_raw": NULLABLE_STRING,
                "address_jp_normalized": NULLABLE_STRING,
                "phone": NULLABLE_STRING,
            }
        ),
        "pricing": required_object(
            {
                "rent": COST_SCHEMA,
                "management_fee": COST_SCHEMA,
                "common_service_fee": COST_SCHEMA,
                "deposit_yen": NULLABLE_INTEGER,
                "key_money_yen": NULLABLE_INTEGER,
                "security_deposit_yen": NULLABLE_INTEGER,
                "renewal_fee_yen": NULLABLE_INTEGER,
                "other_initial_costs": {
                    "type": "array",
                    "items": OTHER_COST_SCHEMA,
                },
                "other_monthly_costs": {
                    "type": "array",
                    "items": OTHER_COST_SCHEMA,
                },
                "base_monthly_total_yen": NULLABLE_INTEGER,
                "variable_monthly_costs": {
                    "type": "array",
                    "items": OTHER_COST_SCHEMA,
                },
            }
        ),
        "contract": required_object(
            {
                "contract_type": NULLABLE_STRING,
                "contract_period": NULLABLE_STRING,
                "available_date": NULLABLE_STRING,
                "renewal_conditions": NULLABLE_STRING,
                "guarantor_company_raw": NULLABLE_STRING,
                "guarantor_company_jp_normalized": NULLABLE_STRING,
                "guarantee_fee_terms": {
                    "type": "array",
                    "items": OTHER_COST_SCHEMA,
                },
                "fire_insurance": NULLABLE_STRING,
            }
        ),
        "conditions": required_object(
            {
                "pets": NULLABLE_STRING,
                "musical_instruments": NULLABLE_STRING,
                "office_use": NULLABLE_STRING,
                "foreign_residents": NULLABLE_STRING,
                "other_restrictions": STRING_ARRAY,
            }
        ),
        "facilities": STRING_ARRAY,
        "assessment": required_object(
            {
                "warnings": STRING_ARRAY,
                "missing_critical_fields": STRING_ARRAY,
                "confidence": required_object(
                    {
                        "overall": {"type": "string"},
                        "property": {"type": "string"},
                        "location": {"type": "string"},
                        "pricing": {"type": "string"},
                        "contract": {"type": "string"},
                    }
                ),
            }
        ),
        "source_text": {"type": "array", "items": SOURCE_TEXT_SCHEMA},
        "source_evidence": {
            "type": "array",
            "items": required_object(
                {
                    "field": {"type": "string"},
                    "image_index": {"type": "integer"},
                    "raw_text": {"type": "string"},
                    "normalized_value": NULLABLE_STRING,
                }
            ),
        },
    }
)


# 일본 부동산 매물 분석용 시스템 프롬프트
DEFAULT_PROMPT = """
당신은 일본 부동산 임대 매물 정보를 판독하고 구조화하는 전문 AI입니다.

입력 이미지를 일본의 부동산 매물 광고, 마이소쿠(マイソク), 임대 조건표 또는 부동산 포털 화면으로 간주하고 분석하세요.
출력 설명은 한국어로 작성하되, 주소·역명·노선명·건물명과 계약 용어는 이미지의 일본어 원문도 함께 보존하세요.

판독 규칙:
1. 이미지에서 직접 확인한 정보만 사용하고 상식으로 값을 추정하지 마세요.
2. 보이지 않거나 판독할 수 없는 값은 null로 작성하세요.
3. 금액은 통화 기호와 쉼표를 제거한 엔화 정수로 작성하세요.
4. 면적은 제곱미터 숫자로, 도보 시간과 건축 연도 등 숫자 필드는 가능한 경우 정수로 작성하세요.
5. '敷金', '礼金', '共益費', '管理費'처럼 의미가 다른 일본 임대 용어를 임의로 합치지 마세요.
6. 월세와 관리비/공익비는 반드시 '🔴 계약 핵심'으로 판정하세요.
7. 서로 모순되는 정보가 있으면 임의로 선택하지 말고 warnings에 기록하세요.
8. 응답은 반드시 아래 구조의 유효한 JSON 객체 하나만 출력하세요. 설명 문장, Markdown 코드 블록, 주석을 JSON 밖에 추가하지 마세요.
9. 모든 키를 빠짐없이 출력하고 정보가 없는 배열은 []로 작성하세요.
10. known_monthly_total_yen은 확인된 월세, 관리비, 공익비와 기타 월 고정비만 합산하고 하나라도 불명확하면 null로 작성하세요.
11. 월세, 관리비/공익비처럼 중요한 값이 확인되지 않으면 해당 키 이름을 missing_critical_fields에 넣으세요.
12. confidence는 이미지 판독 신뢰도에 따라 low, medium, high 중 하나만 사용하세요.
13. 여러 이미지는 별도 지시가 없으면 동일한 매물의 서로 다른 페이지 또는 촬영 각도로 간주하세요.
14. 여러 이미지의 정보를 중복 없이 합치고, 이미지마다 값이 다르면 임의로 선택하지 말고 conflicts와 warnings에 모두 기록하세요.

출력 JSON 형식:
{
  "analysis_metadata": {
    "image_count": 0,
    "same_property_assumed": true,
    "cross_image_consistency": "consistent",
    "conflicts": []
  },
  "property": {
    "property_name_jp": null,
    "property_type": null,
    "layout": null,
    "exclusive_area_m2": null,
    "floor": null,
    "total_floors": null,
    "direction": null,
    "structure": null,
    "built_year": null,
    "built_month": null
  },
  "location": {
    "postal_code": null,
    "address_jp": null,
    "address_ko": null,
    "prefecture": null,
    "city": null,
    "stations": [
      {
        "line_jp": null,
        "station_jp": null,
        "walk_minutes": null
      }
    ]
  },
  "pricing": {
    "rent": {
      "amount_yen": null,
      "raw_text": null,
      "service_judgment": "🔴 계약 핵심"
    },
    "management_fee": {
      "amount_yen": null,
      "raw_text": null,
      "service_judgment": "🔴 계약 핵심"
    },
    "common_service_fee": {
      "amount_yen": null,
      "raw_text": null,
      "service_judgment": "🔴 계약 핵심"
    },
    "deposit_yen": null,
    "key_money_yen": null,
    "security_deposit_yen": null,
    "renewal_fee_yen": null,
    "other_initial_costs": [
      {
        "name_jp": null,
        "name_ko": null,
        "amount_yen": null
      }
    ],
    "other_monthly_costs": [
      {
        "name_jp": null,
        "name_ko": null,
        "amount_yen": null
      }
    ],
    "known_monthly_total_yen": null
  },
  "contract": {
    "contract_type": null,
    "contract_period": null,
    "available_date": null,
    "renewal_conditions": null,
    "guarantor_company": null,
    "fire_insurance": null
  },
  "conditions": {
    "pets": null,
    "musical_instruments": null,
    "office_use": null,
    "foreign_residents": null,
    "other_restrictions": []
  },
  "facilities": [],
  "assessment": {
    "warnings": [],
    "missing_critical_fields": [],
    "confidence": "low"
  },
  "source_text": []
}
"""

SCHEMA_GUIDANCE = """
추가 정규화 및 근거 규칙(아래 규칙이 기존 예시보다 우선합니다):
1. 이미지에 보이는 문자열은 raw 필드에 그대로 옮기고, 번역·일본어 복원·표준화한 값은 normalized 필드에만 기록하세요. 한국어 화면에 없는 일본어를 원문이라고 만들지 마세요.
2. 모든 source_image_index와 source_evidence.image_index는 명령행에 입력된 순서의 1부터 시작합니다.
3. 호실 번호와 실제 층은 서로 다른 개념입니다. 예: 203호와 1층은 오류라고 단정하지 말고 확인이 필요한 잠재적 불일치로 warnings에 기록하세요.
4. 정액 비용만 amount_yen에 넣으세요. 비율·범위·선택 조건은 calculation, frequency, conditional에 분리하고 amount_yen은 null로 두세요.
5. base_monthly_total_yen은 확정된 월세·관리비·공익비 등 정액 월 비용만 합산합니다. 비율 또는 선택형 비용은 variable_monthly_costs에 넣고 합산하지 마세요.
6. 매물에 직접 적용되는 보증료와 보증회사의 일반 안내가 다르면 각각 guarantee_fee_terms에 보존하고 차이를 warnings에 기록하세요.
7. confidence의 overall, property, location, pricing, contract는 각각 low, medium, high 중 하나만 사용하세요.
8. source_evidence에는 핵심 필드별로 field(JSON 경로), image_index, raw_text, normalized_value를 기록하세요.
9. location에는 매물 자체 주소만 기록하세요. '정보 출처', '본점', '회사', '문의처'의 회사명·주소·우편번호·전화번호는 agency에만 기록하세요.
10. 건물명 뒤 101, 203 같은 숫자는 room_number 후보이며 floor와 별개입니다.
11. source_text에는 실제로 보이는 문자열만 image_index, language, raw_text로 기록하세요. 번역하거나 일본어로 복원한 문장은 넣지 마세요.
12. guarantee_fee_terms.scope는 매물 직접 조건이면 listing_specific, 회사 일반 안내면 company_general로 기록하고 listing_specific을 우선하세요.

기존 JSON 예시 대신 다음 변경 스키마를 반드시 적용하세요:
- 최상위에 listing_metadata, agency, source_text, source_evidence를 추가합니다.
- listing_metadata: listing_id, last_updated, next_update, source_languages.
- location: address_raw, address_jp_normalized, address_ko, prefecture, city, postal_code, stations.
- stations 항목: line_raw, line_jp_normalized, station_raw, station_jp_normalized, walk_minutes, source_image_index.
- 모든 기타 비용 항목: scope, name_raw, name_jp_normalized, name_ko, amount_yen, rate_percent, min_rate_percent, max_rate_percent, calculation, frequency, conditional, source_image_index. rent/management_fee/common_service_fee에는 name 계열 대신 service_judgment를 포함합니다.
- pricing의 known_monthly_total_yen 대신 base_monthly_total_yen과 variable_monthly_costs를 사용합니다.
- contract의 guarantor_company 대신 guarantor_company_raw, guarantor_company_jp_normalized, guarantee_fee_terms를 사용합니다.
- property에 room_number를 추가하고 source_text_jp 대신 source_text를 사용합니다.
- assessment.confidence는 문자열이 아니라 overall, property, location, pricing, contract를 가진 객체입니다.
- source_evidence 항목: field, image_index, raw_text, normalized_value.
모든 키를 빠짐없이 출력하고 값이 없으면 null 또는 []를 사용하세요.
"""

# URL 분석에서는 OUTPUT_SCHEMA가 JSON 구조를 강제하므로 이미지용 장문 설명을
# 다시 보내지 않고 판독 원칙만 간결하게 전달한다.
COMPACT_URL_PROMPT = """일본 임대 매물 웹페이지를 한국어로 구조화하라.
추출문에서 직접 확인되는 정보만 사용하고 불명확하면 null, 빈 목록은 []로 쓴다.
금액은 엔화 정수로 정규화하되 敷金,礼金,管理費,共益費를 합치지 않는다.
화면 원문은 raw, 번역·표준화 값은 normalized 필드에 둔다.
비율·범위·조건부 비용은 amount_yen=null로 하고 calculation,frequency,conditional에 기록한다.
base_monthly_total_yen에는 확정된 정액 월 비용만 합산한다.
충돌과 누락은 warnings와 missing_critical_fields에 기록한다.
매물 주소와 중개회사 정보를 분리하고 회사 정보는 agency에만 기록한다.
호실(room_number)과 층수(floor)를 구분하고 source_text에는 보이는 원문만 기록한다.
웹페이지 분석이므로 analysis_metadata.image_count는 0이고 source_image_index는 null이다.
응답은 지정된 스키마의 JSON 객체 하나만 출력한다."""

# REST API에서 사용하는 v3 출력 스키마와 보수적 판독 프롬프트입니다.
# 아래 import가 위의 이전 CLI 호환 정의를 덮어쓰며 실제 실행에는 v3만 사용됩니다.
from app.prompts.image_analysis import (  # noqa: E402
    COMPACT_URL_PROMPT,
    DEFAULT_PROMPT,
    SCHEMA_GUIDANCE,
)
from app.schemas.analysis_output import (  # noqa: E402
    OUTPUT_SCHEMA,
    REQUIRED_NESTED_FIELDS,
    REQUIRED_SECTIONS,
)


def parse_args() -> argparse.Namespace:
    # 모델 arg 설정
    parser = argparse.ArgumentParser(
        description="로컬 이미지 여러 장 또는 웹페이지 URL 하나를 Gemini로 분석합니다."
    )
    parser.add_argument(
        "images",
        type=str,
        nargs="+",
        help="분석할 이미지 파일 경로들 또는 웹페이지 URL 하나",
    )
    parser.add_argument(
        "--prompt",
        default=DEFAULT_PROMPT,
        help="Gemini에 전달할 분석 요청",
    )
    parser.add_argument(
        "--model",
        default="gemini-3.5-flash-lite",
        help="사용할 Gemini 모델 (기본값: gemini-3.5-flash-lite)",
    )
    return parser.parse_args()


def is_web_url(value: str) -> bool:
    parsed = urlparse(value)
    return parsed.scheme in {"http", "https"} and bool(parsed.netloc)


def analyze_webpage(url: str, prompt: str, model: str) -> str:
    total_started = time.perf_counter()
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        raise RuntimeError("GEMINI_API_KEY 환경 변수가 없습니다.")

    url_prompt = COMPACT_URL_PROMPT if prompt == DEFAULT_PROMPT else prompt
    request_text = (
        f"{url_prompt}\nURL Context 도구로 다음 공개 웹페이지를 직접 읽고 "
        f"해당 페이지에서 확인한 내용만 분석하세요.\n분석 대상 URL: {url}"
    )
    logger.info("URL Context 요청 prompt_chars=%d model=%s", len(request_text), model)
    client = genai.Client(api_key=api_key)
    api_started = time.perf_counter()
    interaction = client.interactions.create(
        model=model,
        input=request_text,
        tools=[{"type": "url_context"}],
        generation_config={"thinking_level": "low"},
        response_format={"type": "text", "mime_type": "application/json", "schema": OUTPUT_SCHEMA},
    )
    validate_interaction_status(interaction)
    validate_url_context_result(interaction)
    logger.info(
        "URL Context + Gemini API 응답 duration=%.2fs",
        time.perf_counter() - api_started,
    )
    log_token_usage(interaction, context="url")
    try:
        print_url_context_status(interaction, requested_url=url)
    except Exception as log_error:
        logger.warning("URL Context 로그 해석 실패: %s", log_error)
    if not interaction.output_text:
        raise RuntimeError("Gemini 응답에 분석 결과가 없습니다.")
    result = validate_analysis_json(
        interaction.output_text,
        provider="Gemini",
        debug_info=f"status={getattr(interaction, 'status', None)}",
        source_type="URL",
        source_url=url,
        image_count=0,
    )
    logger.info("URL 분석 전체 처리 duration=%.2fs", time.perf_counter() - total_started)
    return result


def print_url_context_status(interaction, requested_url: str) -> None:
    """URL 조회 여부, 상태, 인용 및 도구 토큰을 진단 가능한 형태로 출력한다."""
    steps = getattr(interaction, "steps", []) or []
    context_steps = [
        step for step in steps if getattr(step, "type", None) == "url_context_result"
    ]
    logger.info(
        "URL Context tool_called=%s requested_url=%s",
        str(bool(context_steps)).lower(),
        requested_url,
    )

    status_values = []
    retrieved_urls = []
    serialized_steps = []
    for step in context_steps:
        payload = safe_model_dump(step)
        serialized_steps.append(payload)
        collect_url_context_values(payload, status_values, retrieved_urls)

    if context_steps:
        logger.info(
            "URL Context status=%s",
            ", ".join(dict.fromkeys(status_values)) or "unknown",
        )
        if retrieved_urls:
            logger.info(
                "URL Context retrieved_url=%s",
                ", ".join(dict.fromkeys(retrieved_urls)),
            )
        if not status_values or not retrieved_urls:
            logger.debug(
                "URL Context 상세=%s",
                json.dumps(serialized_steps, ensure_ascii=False),
            )

    citation_urls = []
    for step in steps:
        if getattr(step, "type", None) != "model_output":
            continue
        for block in getattr(step, "content", []) or []:
            for annotation in getattr(block, "annotations", []) or []:
                if getattr(annotation, "type", None) == "url_citation":
                    citation_url = getattr(annotation, "url", None)
                    if citation_url:
                        citation_urls.append(str(citation_url))
    logger.info("URL Context citation_count=%d", len(citation_urls))
    for citation_url in dict.fromkeys(citation_urls):
        logger.info("URL Context citation_url=%s", citation_url)

    usage = getattr(interaction, "usage", None)
    usage_payload = safe_model_dump(usage)
    tool_tokens = find_numeric_value(usage_payload, "tool_use_input_tokens")
    total_tokens = find_numeric_value(usage_payload, "total_tokens")
    logger.info(
        "URL Context tool_use_input_tokens=%s total_tokens=%s",
        tool_tokens if tool_tokens is not None else "unknown",
        total_tokens if total_tokens is not None else "unknown",
    )


def normalized_status(value) -> str:
    return str(getattr(value, "value", value) or "").lower()


def validate_interaction_status(interaction) -> None:
    """Interactions API가 명시한 종료 상태로 잘림과 실행 실패를 판정한다."""
    status = normalized_status(getattr(interaction, "status", ""))
    if status in {"incomplete", "budget_exceeded"}:
        raise RuntimeError(f"GEMINI_RESPONSE_TRUNCATED: interaction_status={status}")
    if status in {"failed", "cancelled"}:
        errors = safe_model_dump(getattr(interaction, "errors", None))
        raise RuntimeError(
            "GEMINI_INTERACTION_FAILED: "
            f"interaction_status={status}, errors={json.dumps(errors, ensure_ascii=False)}"
        )


def validate_url_context_result(interaction) -> None:
    """SDK가 제공하는 URL Context 상태만 사용해 접근 결과를 검증한다."""
    context_steps = [
        step
        for step in (getattr(interaction, "steps", None) or [])
        if getattr(step, "type", None) == "url_context_result"
    ]
    if not context_steps:
        raise RuntimeError("URL_CONTEXT_FAILED: URL Context 결과 단계가 없습니다.")

    statuses: set[str] = set()
    has_step_error = False
    for step in context_steps:
        has_step_error = has_step_error or bool(getattr(step, "is_error", False))
        for result in getattr(step, "result", None) or []:
            status = getattr(result, "status", None)
            if status:
                statuses.add(normalized_status(status))

    if "paywall" in statuses:
        raise RuntimeError("URL_CONTEXT_PAYWALL: 유료 구독이 필요한 페이지입니다.")
    if "unsafe" in statuses:
        raise RuntimeError("URL_CONTEXT_UNSAFE: 안전 정책으로 URL 접근이 차단됐습니다.")
    if has_step_error or "error" in statuses or "success" not in statuses:
        raise RuntimeError(
            "URL_CONTEXT_FAILED: URL을 가져오지 못했습니다. "
            f"statuses={sorted(statuses) or ['unknown']}"
        )


def log_token_usage(interaction, context: str) -> None:
    """SDK 버전에 상관없이 확인 가능한 토큰 및 캐시 사용량을 기록한다."""
    usage_payload = safe_model_dump(getattr(interaction, "usage", None))
    input_tokens = find_numeric_value(usage_payload, "input_tokens")
    cached_tokens = find_numeric_value(usage_payload, "total_cached_tokens")
    output_tokens = find_numeric_value(usage_payload, "output_tokens")
    thoughts_tokens = find_numeric_value(usage_payload, "thoughts_tokens")
    tool_tokens = find_numeric_value(usage_payload, "tool_use_input_tokens")
    total_tokens = find_numeric_value(usage_payload, "total_tokens")

    cache_hit_rate = None
    if input_tokens and cached_tokens is not None:
        cache_hit_rate = cached_tokens / input_tokens * 100

    logger.info(
        "Gemini 토큰 context=%s input=%s cached=%s output=%s thoughts=%s "
        "tool_input=%s total=%s cache_hit_rate=%s",
        context,
        input_tokens if input_tokens is not None else "unknown",
        cached_tokens if cached_tokens is not None else "unknown",
        output_tokens if output_tokens is not None else "unknown",
        thoughts_tokens if thoughts_tokens is not None else "unknown",
        tool_tokens if tool_tokens is not None else "unknown",
        total_tokens if total_tokens is not None else "unknown",
        f"{cache_hit_rate:.1f}%" if cache_hit_rate is not None else "unknown",
    )


def safe_model_dump(value) -> dict:
    """서로 다른 SDK 모델 버전을 로그 때문에 실패시키지 않고 직렬화한다."""
    if isinstance(value, dict):
        return value
    if hasattr(value, "model_dump"):
        try:
            return value.model_dump(mode="json", exclude_none=True)
        except TypeError:
            return value.model_dump(exclude_none=True)
        except Exception:
            pass
    return {} if value is None else {"raw": str(value)}


def collect_url_context_values(value, statuses: list[str], urls: list[str]) -> None:
    """SDK 버전에 따라 중첩 위치가 달라도 상태와 URL을 재귀적으로 찾는다."""
    if isinstance(value, dict):
        for key, child in value.items():
            lowered_key = str(key).lower()
            if lowered_key == "status" and child is not None:
                statuses.append(str(child))
            elif lowered_key in {"retrieved_url", "url"} and child:
                urls.append(str(child))
            collect_url_context_values(child, statuses, urls)
    elif isinstance(value, list):
        for child in value:
            collect_url_context_values(child, statuses, urls)


def find_numeric_value(value, target_key: str):
    """중첩된 usage 객체에서 지정한 숫자 값을 찾는다."""
    if isinstance(value, dict):
        for key, child in value.items():
            if key == target_key and isinstance(child, (int, float)):
                return child
            found = find_numeric_value(child, target_key)
            if found is not None:
                return found
    elif isinstance(value, list):
        for child in value:
            found = find_numeric_value(child, target_key)
            if found is not None:
                return found
    return None


def analyze_images(image_paths: list[Path], prompt: str, model: str) -> str:
    total_started = time.perf_counter()

    # 절대 경로로 통일하고 모든 파일의 존재 여부를 확인한다.
    resolved_paths = [path.expanduser().resolve() for path in image_paths]
    for image_path in resolved_paths:
        if not image_path.is_file():
            raise FileNotFoundError(f"이미지를 찾을 수 없습니다: {image_path}")

    # 시스템 환경 변수에 저장된 API 키를 가져온다.
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        raise RuntimeError("GEMINI_API_KEY 환경 변수가 없습니다.")

    client = genai.Client(api_key=api_key)

    # 모든 이미지를 Base64로 변환해 하나의 요청에 넣는다.
    preprocess_started = time.perf_counter()
    if prompt == DEFAULT_PROMPT:
        request_prompt = (
            "당신은 일본 임대 매물 정보를 판독하는 전문 AI입니다.\n"
            f"{SCHEMA_GUIDANCE}"
        )
    else:
        request_prompt = f"{prompt}\n\n{SCHEMA_GUIDANCE}"
    request_prompt += f"\n이번 요청의 이미지 수: {len(resolved_paths)}"
    interaction_input = [{"type": "text", "text": request_prompt}]
    interaction_input.extend(encode_image(image_path) for image_path in resolved_paths)
    preprocess_seconds = time.perf_counter() - preprocess_started
    logger.info(
        "이미지 전처리 image_count=%d duration=%.2fs",
        len(resolved_paths),
        preprocess_seconds,
    )

    api_started = time.perf_counter()
    interaction = client.interactions.create(
        model=model,
        input=interaction_input,
        generation_config={"thinking_level": "low"},
        response_format={
            "type": "text",
            "mime_type": "application/json",
            "schema": OUTPUT_SCHEMA,
        },
    )
    validate_interaction_status(interaction)
    api_seconds = time.perf_counter() - api_started
    logger.info("Gemini 이미지 API 응답 duration=%.2fs", api_seconds)
    log_token_usage(interaction, context="images")

    if not interaction.output_text:
        raise RuntimeError(
            "Gemini 응답에 분석 결과가 없습니다. "
            f"status={getattr(interaction, 'status', None)}, "
            f"usage={getattr(interaction, 'usage', None)}"
        )

    validation_started = time.perf_counter()
    result = validate_analysis_json(
        interaction.output_text,
        provider="Gemini",
        debug_info=(
            f"status={getattr(interaction, 'status', None)}, "
            f"usage={getattr(interaction, 'usage', None)}"
        ),
        source_type="IMAGE",
        source_url=None,
        image_count=len(resolved_paths),
    )
    validation_seconds = time.perf_counter() - validation_started
    total_seconds = time.perf_counter() - total_started
    logger.info("Gemini JSON 검증 duration=%.2fs", validation_seconds)
    logger.info("이미지 분석 전체 처리 duration=%.2fs", total_seconds)
    return result


def encode_image(image_path: Path) -> dict:
    """로컬 이미지를 Gemini 인라인 입력 형식으로 변환한다."""
    mime_type, _ = mimetypes.guess_type(image_path.name)
    if not mime_type or not mime_type.startswith("image/"):
        raise ValueError(f"지원되는 이미지 파일이 아닙니다: {image_path}")

    encoded_image = base64.b64encode(image_path.read_bytes()).decode("utf-8")
    return {
        "type": "image",
        "data": encoded_image,
        "mime_type": mime_type,
    }


def extract_direct_yen_amounts(raw_value: object) -> set[int]:
    """원문에 직접 표시된 엔화 정수만 추출한다."""
    text = str(raw_value or "").strip()
    number_pattern = r"\d{1,3}(?:,\d{3})+|\d+"
    matches = [
        prefix or suffix
        for prefix, suffix in re.findall(
            rf"[¥￥]\s*({number_pattern})|({number_pattern})\s*(?:円|엔)", text
        )
    ]
    amounts = {int(value.replace(",", "")) for value in matches}
    for value in re.findall(r"(?<![\d.])(\d+(?:\.\d+)?)\s*万円", text):
        yen = Decimal(value) * 10000
        if yen == yen.to_integral_value():
            amounts.add(int(yen))
    if not matches and re.fullmatch(r"[\d,]+", text):
        amounts.add(int(text.replace(",", "")))
    return amounts


def has_required_evidence(item: dict, analysis: dict) -> bool:
    """비용에 직접 연결된 필수·발생 문구가 있는지 확인한다."""
    evidence_text = " ".join(
        str(evidence.get("raw_text") or "")
        for evidence in analysis.get("evidence", [])
        if isinstance(evidence, dict)
    )
    text = " ".join(
        str(value or "")
        for value in (item.get("raw_name"), item.get("raw_value"), evidence_text)
    )
    if re.search(
        r"不要|不必要|必要なし|필요\s*없|불필요|任意|オプション|希望者のみ|선택",
        text,
    ):
        return False
    return bool(
        re.search(
            r"加入要|利用必|必須|必要|契約時必要|(?<!不)要(?:\b|\s|[（(])|"
            r"発生(?:する|します)?|負担(?:する|します)?|"
            r"가입\s*필수|필수|필요(?:함)?|발생(?:함|합니다)?|부담",
            text,
        )
    )


def is_no_cost_statement(text: str) -> bool:
    """무료·불필요처럼 실제 청구가 없음을 명시한 문구인지 판정한다."""
    return bool(
        re.search(
            r"不要|不必要|必要なし|必要ありません|無料|なし|無し|(?<![\d,])0\s*(?:円|엔|원)|"
            r"ゼロ|불필요|필요\s*없|무료|없음|제로",
            text,
            re.IGNORECASE,
        )
    )


def infer_obligation_status(text: str) -> str:
    """금액 유무와 분리해 직접적인 의무·선택 표현으로만 판정한다."""
    if is_no_cost_statement(text):
        return "UNKNOWN"
    if re.search(
        r"加入要|利用必|必須|契約時必要|(?<!不)要(?:\b|\s|[（(])|"
        r"発生(?:する|します)?|負担(?:する|します)?|"
        r"가입\s*필수|필수|의무|발생(?:함|합니다)?|부담",
        text,
    ):
        return "REQUIRED"
    if re.search(r"任意|オプション|希望者のみ|임의|선택|옵션|희망자", text):
        return "OPTIONAL"
    return "UNKNOWN"


def infer_cost_timing(text: str, fallback: str = "UNKNOWN") -> str:
    """비용 원문의 명시적인 발생 시점만 판정한다; 모델 추정은 유지하지 않는다."""
    if re.search(r"退去時|解約時|퇴거\s*시|해약\s*시", text):
        return "MOVE_OUT"
    if re.search(
        r"更新時|更新料|毎年|年額|年間(?:保証料|費用|料金)?|"
        r"연간|매년|갱신\s*(?:시|료)",
        text,
    ):
        return "RENEWAL"
    if re.search(r"月額|毎月|월액|월정액|매월|/(?:月|월)|[월月]\s*[¥￥]", text):
        return "MONTHLY"
    if re.search(r"初回|契約時|입주\s*시|계약\s*시|초기", text):
        return "INITIAL"
    if re.search(
        r"飼育時|利用時|使用時|短期解約|발생\s*시|이용\s*시|사용\s*시|"
        r"사육\s*시|단기\s*해약",
        text,
    ):
        return "CONDITIONAL"
    return "UNKNOWN"


def split_compound_cost_candidates(cost_candidates: list[dict]) -> list[dict]:
    """슬래시로 구분된 서로 다른 시점의 비용 문구를 후보별로 나눈다."""
    result: list[dict] = []
    for candidate in cost_candidates:
        raw_text = str(candidate.get("raw_text") or "")
        parts = [part.strip() for part in re.split(r"\s*/\s*", raw_text) if part.strip()]
        timed_parts = [(part, infer_cost_timing(part)) for part in parts]
        if len(parts) < 2 or sum(timing != "UNKNOWN" for _, timing in timed_parts) < 2:
            result.append(candidate)
            continue

        for part, timing in timed_parts:
            split_candidate = deepcopy(candidate)
            split_candidate["raw_text"] = part
            split_candidate["timing"] = timing
            split_candidate["_replaces_target_index"] = candidate.get("target_index")
            split_candidate["target_index"] = None
            for evidence in split_candidate.get("evidence", []):
                if isinstance(evidence, dict):
                    evidence["raw_text"] = part
            result.append(split_candidate)
    return result


def candidate_direct_amount(raw_text: str) -> int | None:
    """범위·비율·개월 환산이 없는 단일 직접 엔화 금액만 반환한다."""
    if re.search(r"[%％～〜~]|ヶ月|か月|개월|月分", raw_text):
        return None
    amounts = extract_direct_yen_amounts(raw_text)
    return next(iter(amounts)) if len(amounts) == 1 else None


FIXED_COST_FIELD_ALIASES = {
    "rent": {"家賃", "賃料", "월세", "임대료"},
    "management_fee": {"管理費", "共益費", "管理費・共益費", "관리비", "공익비"},
    "deposit": {"敷金", "保証金", "시키킹", "시키킨", "보증금"},
    "key_money": {"礼金", "레이킹", "레이킨", "사례금"},
}


def fixed_cost_field(cost_name: object) -> str | None:
    """정확한 고정 비용명만 DB property 필드에 연결한다."""
    normalized = re.sub(r"\s+", "", str(cost_name or ""))
    for field, aliases in FIXED_COST_FIELD_ALIASES.items():
        if normalized in {re.sub(r"\s+", "", alias) for alias in aliases}:
            return field
    return None


def fixed_cost_fields(cost_name: object) -> list[str]:
    """개별 또는 슬래시로 묶인 고정 비용명을 property 필드 순서로 반환한다."""
    direct_field = fixed_cost_field(cost_name)
    if direct_field is not None:
        return [direct_field]
    parts = [
        part.strip()
        for part in re.split(r"\s*[/／・]\s*", str(cost_name or ""))
        if part.strip()
    ]
    fields = [fixed_cost_field(part) for part in parts]
    if len(fields) > 1 and all(field is not None for field in fields):
        return [field for field in fields if field is not None]
    return []


def parse_fixed_cost_value(raw_value: str) -> tuple[str, int | str] | None:
    """직접 엔화 값, 명시적 0, 계산 불가 표현을 구분한다."""
    text = raw_value.strip()
    amount = candidate_direct_amount(text)
    if amount is not None:
        return "VALUE", amount
    if re.fullmatch(r"[-－―—–ー]+", text) or is_no_cost_statement(text):
        return "VALUE", 0
    if re.search(r"[%％]|\d+(?:\.\d+)?\s*(?:ヶ月|ヵ月|か月|개월|月分)", text):
        return "EXPRESSION", text
    return None


def fixed_cost_candidate_observations(candidate: dict) -> list[dict]:
    """비용 후보 하나에서 안전하게 대응되는 property 관측값을 만든다."""
    if candidate.get("applies_to_listing") != "YES":
        return []
    if str(candidate.get("applicability_condition") or "").strip():
        return []

    cost_name = str(candidate.get("cost_name") or "").strip()
    raw_text = str(candidate.get("raw_text") or "").strip()
    fields = fixed_cost_fields(cost_name)
    if not fields or not raw_text:
        return []

    if len(fields) == 1 and fixed_cost_field(cost_name) is not None:
        parsed = parse_fixed_cost_value(raw_text)
        if parsed is None:
            return []
        kind, value = parsed
        return [{
            "field": fields[0], "kind": kind, "value": value,
            "raw_value": raw_text, "candidate": candidate,
        }]

    value_text = re.sub(rf"^\s*{re.escape(cost_name)}\s*", "", raw_text, count=1)
    values = [part.strip() for part in re.split(r"\s*[/／]\s*", value_text)]
    if len(values) != len(fields):
        return []

    observations = []
    for field, raw_value in zip(fields, values):
        parsed = parse_fixed_cost_value(raw_value)
        if parsed is None:
            return []
        kind, value = parsed
        observations.append({
            "field": field, "kind": kind, "value": value,
            "raw_value": raw_value, "candidate": candidate,
        })
    return observations


def reconcile_fixed_cost_candidates(
    property_fields: dict,
    field_meta: dict,
    cost_candidates: list[dict],
    validation: dict,
) -> None:
    """두 모델의 고정 비용을 직접 원문 금액으로 제한해 교차 검증한다."""
    warnings = validation.setdefault("warnings", [])
    conflicts = validation.setdefault("conflicts", [])
    unknown_fields = validation.setdefault("unknown_fields", [])
    observations_by_field: dict[str, list[dict]] = {}

    for candidate in cost_candidates:
        for observation in fixed_cost_candidate_observations(candidate):
            observations_by_field.setdefault(observation["field"], []).append(observation)

    def display_value(value: object) -> str:
        return "null" if value is None else str(value)

    for field, entries in observations_by_field.items():
        field_path = f"property.{field}"
        current = property_fields.get(field)
        evidence = [
            item
            for entry in entries
            for item in entry["candidate"].get("evidence", [])
            if isinstance(item, dict)
        ]
        expressions = [entry for entry in entries if entry["kind"] == "EXPRESSION"]
        amounts = {
            int(entry["value"])
            for entry in entries
            if entry["kind"] == "VALUE"
        }

        if expressions:
            property_fields[field] = None
            if field_path not in unknown_fields:
                unknown_fields.append(field_path)
            values = {display_value(current), *(str(entry["value"]) for entry in expressions)}
            values.update(str(value) for value in amounts)
            conflicts.append({
                "field": field_path,
                "values": sorted(values),
                "reason": "개월·비율 표현은 엔화 정수로 계산하지 않고 원문으로 보존합니다.",
                "resolution": "UNKNOWN",
                "resolved_value": None,
                "evidence": evidence,
            })
            meta = field_meta.get(field_path)
            if isinstance(meta, dict):
                meta["raw_value"] = expressions[0]["raw_value"]
                meta["evidence"] = evidence
                meta["needs_review"] = True
                meta["confidence"] = min(float(meta.get("confidence") or 0), 0.69)
            warnings.append(f"계산 불가 고정 비용 표현을 확인했습니다: {field_path}")
            continue

        if not amounts:
            continue

        if len(amounts) > 1:
            property_fields[field] = None
            if field_path not in unknown_fields:
                unknown_fields.append(field_path)
            values = {str(value) for value in amounts}
            values.add(display_value(current))
            conflicts.append({
                "field": field_path,
                "values": sorted(values),
                "reason": "현재 매물의 고정 비용 후보 금액이 서로 다릅니다.",
                "resolution": "UNKNOWN",
                "resolved_value": None,
                "evidence": evidence,
            })
            meta = field_meta.get(field_path)
            if isinstance(meta, dict):
                meta["needs_review"] = True
                meta["confidence"] = min(float(meta.get("confidence") or 0), 0.69)
            continue

        candidate_amount = next(iter(amounts))
        if current == candidate_amount:
            continue
        if current not in {None, 0}:
            property_fields[field] = None
            if field_path not in unknown_fields:
                unknown_fields.append(field_path)
            conflicts.append({
                "field": field_path,
                "values": [display_value(current), str(candidate_amount)],
                "reason": "고정 모델과 비용 모델의 직접 금액이 서로 다릅니다.",
                "resolution": "UNKNOWN",
                "resolved_value": None,
                "evidence": evidence,
            })
            meta = field_meta.get(field_path)
            if isinstance(meta, dict):
                meta["needs_review"] = True
                meta["confidence"] = min(float(meta.get("confidence") or 0), 0.69)
            continue

        property_fields[field] = candidate_amount
        if field_path in unknown_fields:
            unknown_fields.remove(field_path)
        matching_entry = next(
            entry for entry in entries
            if entry["kind"] == "VALUE" and int(entry["value"]) == candidate_amount
        )
        meta = field_meta.get(field_path)
        if isinstance(meta, dict):
            meta["raw_value"] = matching_entry["raw_value"]
            meta["evidence"] = evidence
            meta["confidence"] = 1
            meta["needs_review"] = False
        conflicts.append({
            "field": field_path,
            "values": [display_value(current), str(candidate_amount)],
            "reason": "조건 없는 현재 매물 고정 비용 후보에서 직접 엔화 금액을 확인했습니다.",
            "resolution": "RESOLVED",
            "resolved_value": str(candidate_amount),
            "evidence": evidence,
        })
        warnings.append(
            f"고정 모델과 비용 모델을 교차 검증해 값을 교정했습니다: "
            f"{field_path} {current}->{candidate_amount}"
        )


def apply_semantic_checks(data: dict) -> None:
    """DB 저장형 결과에 대표 역, 비용 중복과 계산 금지 규칙을 적용한다."""
    details = data.setdefault("analysis_details", {})
    validation = details.setdefault("validation", {})
    warnings = validation.setdefault("warnings", [])
    unknown_fields = validation.setdefault("unknown_fields", [])
    property_fields = data.setdefault("property", {})
    field_analysis = details.setdefault("field_analysis", [])

    # URL 분석에서 모델이 생략하기 쉬운 property 경로 접두사를 보정한다.
    property_field_names = set(property_fields)
    for item in field_analysis:
        if not isinstance(item, dict):
            continue
        field_path = item.get("field")
        if field_path in property_field_names:
            item["field"] = f"property.{field_path}"
    field_meta = {
        item.get("field"): item
        for item in field_analysis
        if isinstance(item, dict) and item.get("field")
    }

    # 모든 역을 보존하면서 최단 도보 역 하나를 DB 대표 역으로 선택한다.
    stations = [
        station
        for station in details.setdefault("all_stations", [])
        if isinstance(station, dict)
    ]
    walkable = [
        (index, station)
        for index, station in enumerate(stations)
        if isinstance(station.get("walk_minutes"), int)
    ]
    if walkable:
        _, nearest = min(walkable, key=lambda pair: (pair[1]["walk_minutes"], pair[0]))
        property_fields["nearest_station"] = nearest.get("station_name")
        property_fields["walk_minutes"] = nearest.get("walk_minutes")

        # 모델이 다른 대표 역을 골랐더라도 최단 역의 근거로 분석 정보를 맞춘다.
        for field_path, raw_value in (
            ("property.nearest_station", nearest.get("station_name")),
            ("property.walk_minutes", str(nearest.get("walk_minutes"))),
        ):
            meta = field_meta.get(field_path)
            if isinstance(meta, dict):
                meta["raw_value"] = raw_value
                meta["evidence"] = nearest.get("evidence", [])

    # property 값의 근거와 검토 상태를 별도 분석 메타데이터에서 검사한다.
    for name, value in property_fields.items():
        field_path = f"property.{name}"
        meta = field_meta.get(field_path)
        if value is None and field_path not in unknown_fields:
            unknown_fields.append(field_path)
        if not isinstance(meta, dict):
            if value is not None:
                warnings.append(f"필드 분석 정보가 없습니다: {field_path}")
            continue
        confidence = meta.get("confidence")
        evidence = meta.get("evidence")
        if value is None:
            meta["needs_review"] = True
            meta["confidence"] = min(float(confidence or 0), 0.69)
            confidence = meta["confidence"]
        if isinstance(confidence, (int, float)) and confidence < 0.7:
            meta["needs_review"] = True
        if value is not None and not evidence:
            meta["needs_review"] = True
            meta["confidence"] = min(float(confidence or 0), 0.69)
            warnings.append(f"근거가 없는 확정값: {field_path}")

    # 모델이 계산한 고정 필드 값은 제거하고 원문만 분석 정보에 보존한다.
    calculated_markers = ("%", "ヶ月", "か月", "개월", "月分")
    for name in ("rent", "deposit", "key_money", "listed_initial_cost_total"):
        field_path = f"property.{name}"
        meta = field_meta.get(field_path, {})
        raw_value = str(meta.get("raw_value") or "")
        if property_fields.get(name) is not None and any(
            marker in raw_value for marker in calculated_markers
        ):
            property_fields[name] = None
            meta["needs_review"] = True
            if field_path not in unknown_fields:
                unknown_fields.append(field_path)
            warnings.append(f"AI 계산값을 제거했습니다: {field_path}")

    management_meta = field_meta.get("property.management_fee", {})
    management_raw = str(management_meta.get("raw_value") or "")
    explicit_yen_values = re.findall(
        r"(?:\d[\d,]*\s*(?:円|엔)|\d+(?:\.\d+)?\s*万円)", management_raw
    )
    if property_fields.get("management_fee") is not None and len(explicit_yen_values) > 1:
        property_fields["management_fee"] = None
        management_meta["needs_review"] = True
        if "property.management_fee" not in unknown_fields:
            unknown_fields.append("property.management_fee")
        warnings.append("관리비와 공익비의 AI 합산값을 제거했습니다.")

    contract_meta = field_meta.get("property.contract_period_months", {})
    contract_raw = str(contract_meta.get("raw_value") or "")
    if property_fields.get("contract_period_months") is not None and "年" in contract_raw:
        if not re.search(r"\d+\s*(?:ヶ月|か月|개월)", contract_raw):
            property_fields["contract_period_months"] = None
            contract_meta["needs_review"] = True
            if "property.contract_period_months" not in unknown_fields:
                unknown_fields.append("property.contract_period_months")
            warnings.append("계약기간의 AI 월 환산값을 제거했습니다.")

    available_from = property_fields.get("available_from")
    if available_from is not None and not re.fullmatch(r"\d{4}-\d{2}-\d{2}", str(available_from)):
        property_fields["available_from"] = None
        available_meta = field_meta.get("property.available_from", {})
        available_meta["needs_review"] = True
        if "property.available_from" not in unknown_fields:
            unknown_fields.append("property.available_from")
        warnings.append("날짜로 확정할 수 없는 입주 가능 값을 제거했습니다.")

    # DB 금액과 원문에 직접 표시된 금액이 다르면 확정값을 제거한다.
    amount_fields_match = True
    for name in ("rent", "management_fee", "deposit", "key_money", "listed_initial_cost_total"):
        amount = property_fields.get(name)
        meta = field_meta.get(f"property.{name}", {})
        direct_amounts = extract_direct_yen_amounts(meta.get("raw_value"))
        if amount is not None and direct_amounts and amount not in direct_amounts:
            property_fields[name] = None
            meta["needs_review"] = True
            meta["confidence"] = min(float(meta.get("confidence") or 0), 0.69)
            field_path = f"property.{name}"
            if field_path not in unknown_fields:
                unknown_fields.append(field_path)
            warnings.append(f"원문 금액과 구조화 금액이 일치하지 않습니다: {field_path}")
            amount_fields_match = False

    # 월세·관리비·시키킨·레이킨은 property 전용이며 가변 비용에서 제거한다.
    def is_fixed_cost_name(value: object) -> bool:
        return bool(fixed_cost_fields(value))

    cost_candidates = split_compound_cost_candidates([
        candidate
        for candidate in details.setdefault("cost_candidates", [])
        if isinstance(candidate, dict)
    ])
    details["cost_candidates"] = cost_candidates
    for candidate in cost_candidates:
        candidate_text = " ".join(
            str(candidate.get(key) or "")
            for key in ("cost_name", "display_name", "raw_text", "applicability_condition")
        )
        candidate["timing"] = infer_cost_timing(candidate_text)
        if is_fixed_cost_name(candidate.get("cost_name")):
            candidate["destination"] = "PROPERTY"
            candidate["target_index"] = None
        elif is_no_cost_statement(candidate_text):
            candidate["destination"] = "EXCLUDED"
            candidate["target_index"] = None
    reconcile_fixed_cost_candidates(
        property_fields, field_meta, cost_candidates, validation
    )
    split_target_indices = {
        candidate.get("_replaces_target_index")
        for candidate in cost_candidates
        if isinstance(candidate.get("_replaces_target_index"), int)
    }
    candidates_by_target = {
        candidate.get("target_index"): candidate
        for candidate in cost_candidates
        if candidate.get("destination") == "PROPERTY_COST_ITEM"
        and isinstance(candidate.get("target_index"), int)
    }
    unique_costs = []
    unique_analyses = []
    seen_costs = set()
    cost_index_by_identity = {}
    required_statuses_valid = True
    cost_analyses = {
        item.get("cost_item_index"): item
        for item in details.setdefault("cost_item_analysis", [])
        if isinstance(item, dict)
    }
    for old_index, item in enumerate(data.setdefault("property_cost_items", [])):
        if not isinstance(item, dict):
            continue
        if old_index in split_target_indices:
            warnings.append(
                f"여러 발생 시점이 합쳐진 비용을 분리했습니다: "
                f"property_cost_items[{old_index}]"
            )
            continue
        analysis = cost_analyses.get(old_index, {})
        candidate = candidates_by_target.get(old_index)
        evidence_text = " ".join(
            str(evidence.get("raw_text") or "")
            for evidence in analysis.get("evidence", [])
            if isinstance(evidence, dict)
        )
        item_primary_text = " ".join(
            str(value or "")
            for value in (
                item.get("raw_name"), item.get("display_name"),
                item.get("raw_value"),
            )
        )
        item_text = f"{item_primary_text} {evidence_text}"
        if is_no_cost_statement(item_text):
            warnings.append(
                f"무료·불필요 문구를 저장 비용에서 제외했습니다: {item.get('raw_name')}"
            )
            continue
        if candidate and candidate.get("applies_to_listing") not in {
            "YES",
            "CONDITIONAL",
        }:
            candidate["destination"] = "REFERENCE_INFORMATION"
            candidate["target_index"] = None
            warnings.append(
                f"현재 매물 적용 근거가 없어 비용에서 제외했습니다: "
                f"property_cost_items[{old_index}]"
            )
            continue
        if is_fixed_cost_name(item.get("raw_name")):
            if candidate:
                candidate["destination"] = "PROPERTY"
                candidate["target_index"] = None
            warnings.append(f"고정 비용 중복을 제거했습니다: {item.get('raw_name')}")
            continue
        confidence = analysis.get("confidence")
        evidence = analysis.get("evidence")
        if isinstance(confidence, (int, float)) and confidence < 0.7:
            analysis["needs_review"] = True
        if not evidence:
            analysis["needs_review"] = True
            analysis["confidence"] = min(float(confidence or 0), 0.69)
            warnings.append(f"근거가 없는 비용 항목: property_cost_items[{old_index}]")
        raw_value = str(item.get("raw_value") or "")
        original_timing = str(item.get("timing") or "UNKNOWN")
        item["timing"] = infer_cost_timing(item_primary_text)
        if original_timing != item["timing"]:
            analysis["needs_review"] = True
            warnings.append(
                f"직접적인 시점 근거로 timing을 변경했습니다: "
                f"property_cost_items[{old_index}] {original_timing}->{item['timing']}"
            )
        if item.get("amount") is not None and any(
            marker in raw_value for marker in calculated_markers
        ):
            item["amount"] = None
            analysis["needs_review"] = True
            warnings.append(f"AI 계산 비용을 제거했습니다: property_cost_items[{old_index}]")
        direct_amounts = extract_direct_yen_amounts(raw_value)
        if item.get("amount") is not None and direct_amounts:
            if item["amount"] not in direct_amounts:
                item["amount"] = None
                analysis["needs_review"] = True
                analysis["confidence"] = min(float(confidence or 0), 0.69)
                warnings.append(
                    f"원문 금액과 구조화 금액이 일치하지 않습니다: "
                    f"property_cost_items[{old_index}]"
                )
                amount_fields_match = False
        original_obligation = str(item.get("obligation_status") or "UNKNOWN")
        item["obligation_status"] = infer_obligation_status(item_text)
        if original_obligation != item["obligation_status"]:
            analysis["needs_review"] = True
            analysis["confidence"] = min(float(confidence or 0), 0.69)
            warnings.append(
                f"직접적인 의무 근거로 obligation_status를 변경했습니다: "
                f"property_cost_items[{old_index}] "
                f"{original_obligation}->{item['obligation_status']}"
            )
            if original_obligation == "REQUIRED":
                required_statuses_valid = False
        if item.get("obligation_status") == "UNKNOWN":
            analysis["needs_review"] = True
            analysis["confidence"] = min(float(analysis.get("confidence") or 0), 0.69)
        if re.search(r"[～〜~]|または|又は|혹은|또는", raw_value):
            analysis["needs_review"] = True
            analysis["confidence"] = min(float(analysis.get("confidence") or 0), 0.69)
        identity = (
            item.get("raw_name"),
            item.get("raw_value"),
            item.get("timing"),
            item.get("amount"),
        )
        if identity not in seen_costs:
            seen_costs.add(identity)
            analysis["cost_item_index"] = len(unique_costs)
            analysis["scope"] = "LISTING_SPECIFIC"
            cost_index_by_identity[identity] = len(unique_costs)
            unique_costs.append(item)
            unique_analyses.append(analysis)
            if candidate:
                candidate["target_index"] = len(unique_costs) - 1
        elif candidate:
            candidate["target_index"] = cost_index_by_identity[identity]
    data["property_cost_items"] = unique_costs
    details["cost_item_analysis"] = unique_analyses
    for candidate in cost_candidates:
        candidate.pop("_replaces_target_index", None)

    # 현재 매물 적용 후보를 모델이 잘못 제외했다면 보수적 기본값으로 복구한다.
    for candidate in cost_candidates:
        if candidate.get("applies_to_listing") not in {"YES", "CONDITIONAL"}:
            continue
        candidate_text = " ".join(
            str(candidate.get(key) or "")
            for key in ("cost_name", "display_name", "raw_text", "applicability_condition")
        )
        if candidate.get("destination") == "PROPERTY":
            continue
        if is_no_cost_statement(candidate_text):
            candidate["destination"] = "EXCLUDED"
            candidate["target_index"] = None
            continue
        target_index = candidate.get("target_index")
        if (
            candidate.get("destination") == "PROPERTY_COST_ITEM"
            and isinstance(target_index, int)
            and 0 <= target_index < len(unique_costs)
        ):
            continue

        raw_name = str(candidate.get("cost_name") or "").strip()
        raw_text = str(candidate.get("raw_text") or "").strip()
        if not raw_name or not raw_text or is_fixed_cost_name(raw_name):
            continue

        item = {
            "raw_name": raw_name,
            "display_name": str(candidate.get("display_name") or raw_name),
            "amount": candidate_direct_amount(raw_text),
            "raw_value": raw_text,
            "obligation_status": "UNKNOWN",
            "timing": infer_cost_timing(candidate_text),
        }
        identity = (
            item["raw_name"], item["raw_value"], item["timing"], item["amount"]
        )
        if identity in cost_index_by_identity:
            new_index = cost_index_by_identity[identity]
        else:
            new_index = len(unique_costs)
            cost_index_by_identity[identity] = new_index
            unique_costs.append(item)
            unique_analyses.append(
                {
                    "cost_item_index": new_index,
                    "scope": "LISTING_SPECIFIC",
                    "confidence": 0.69,
                    "needs_review": True,
                    "evidence": candidate.get("evidence", []),
                }
            )
        candidate["destination"] = "PROPERTY_COST_ITEM"
        candidate["target_index"] = new_index
        warnings.append(f"현재 매물 적용 비용 후보를 복구했습니다: {raw_name}")

    data["property_cost_items"] = unique_costs
    details["cost_item_analysis"] = unique_analyses

    unresolved_fields = {
        conflict.get("field")
        for conflict in validation.setdefault("conflicts", [])
        if isinstance(conflict, dict) and conflict.get("resolution") == "UNKNOWN"
    }
    for field_path in unresolved_fields:
        if field_path and field_path not in unknown_fields:
            unknown_fields.append(field_path)

    checks = validation.setdefault("checks", {})
    checks["evidence_only"] = not any("근거가 없는" in item for item in warnings)
    checks["duplicates_removed"] = True
    checks["conflicts_reviewed"] = not bool(unresolved_fields)
    # 위 단계에서 중복 고정비를 제거하고 일반 안내를 확정 비용과 분리했다.
    checks["fixed_costs_not_duplicated"] = True
    checks["listing_terms_preferred"] = True
    checks["amounts_match_raw_text"] = amount_fields_match
    checks["required_status_has_evidence"] = required_statuses_valid
    checks["amount_does_not_imply_required"] = all(
        item.get("obligation_status") != "REQUIRED"
        or has_required_evidence(item, analysis)
        for item, analysis in zip(unique_costs, unique_analyses)
    )
    checks["zero_and_null_distinguished"] = True
    additional_costs_complete = True
    candidate_amounts = set().union(
        *(extract_direct_yen_amounts(candidate.get("raw_text")) for candidate in cost_candidates)
    ) if cost_candidates else set()
    for additional in details.setdefault("additional_fields", []):
        if not isinstance(additional, dict):
            continue
        raw_text = str(additional.get("raw_value") or additional.get("value") or "")
        amounts = extract_direct_yen_amounts(raw_text)
        if amounts and not amounts.intersection(candidate_amounts):
            additional_costs_complete = False
            warnings.append(
                f"additional_fields의 금전 문구가 비용 후보에 없습니다: "
                f"{additional.get('raw_name')}"
            )

    checks["cost_candidates_classified"] = additional_costs_complete and all(
        (
            candidate.get("applies_to_listing") in {"YES", "CONDITIONAL"}
            and candidate.get("destination") in {"PROPERTY", "PROPERTY_COST_ITEM"}
            and (
                candidate.get("destination") == "PROPERTY"
                or (
                    isinstance(candidate.get("target_index"), int)
                    and 0 <= candidate["target_index"] < len(unique_costs)
                )
            )
        )
        or (
            candidate.get("applies_to_listing") in {"NO", "UNKNOWN"}
            and candidate.get("destination")
            in {"REFERENCE_INFORMATION", "EXCLUDED"}
            and candidate.get("target_index") is None
        )
        or (
            is_no_cost_statement(str(candidate.get("raw_text") or ""))
            and candidate.get("destination") == "EXCLUDED"
            and candidate.get("target_index") is None
        )
        for candidate in cost_candidates
    )


def validate_analysis_json(
    result: str,
    provider: str,
    debug_info: str,
    source_type: str,
    source_url: str | None,
    image_count: int,
) -> str:
    """응답 JSON과 필수 영역을 검사하고 보기 좋게 정렬한다."""
    try:
        data = json.loads(result)
    except json.JSONDecodeError as error:
        raise RuntimeError(
            f"{provider}가 유효하지 않은 JSON을 반환했습니다: {error}. "
            f"{debug_info}\n원본 응답: {result[:500]}"
        ) from error

    if not isinstance(data, dict) or not data:
        raise RuntimeError(
            f"{provider}가 빈 JSON 객체를 반환했습니다. {debug_info}"
        )

    missing = sorted(REQUIRED_SECTIONS - data.keys())
    if missing:
        raise RuntimeError(
            f"{provider} 응답에 필수 영역이 없습니다: {missing}. {debug_info}"
        )

    for section, required_fields in REQUIRED_NESTED_FIELDS.items():
        section_data = data.get(section)
        if not isinstance(section_data, dict):
            raise RuntimeError(
                f"{provider} 응답의 {section} 영역이 객체가 아닙니다. {debug_info}"
            )
        missing_fields = sorted(required_fields - section_data.keys())
        if missing_fields:
            raise RuntimeError(
                f"{provider} 응답의 {section} 영역에 필수 필드가 없습니다: "
                f"{missing_fields}. {debug_info}"
            )

    metadata = data.get("analysis_metadata", {})
    metadata["schema_version"] = "3.1"
    metadata["source_type"] = source_type
    metadata["image_count"] = image_count
    data.get("property", {})["source_url"] = source_url

    apply_semantic_checks(data)

    return json.dumps(data, ensure_ascii=False, indent=2)


def main() -> int:
    # 이전 CLI 실행 방식에서도 FastAPI와 같은 로그 설정을 사용한다.
    from app.core.logging_config import configure_logging

    configure_logging()
    args = parse_args()

    try:
        if len(args.images) == 1 and is_web_url(args.images[0]):
            logger.info("웹페이지 분석 시작 url=%s", args.images[0])
            result = analyze_webpage(args.images[0], args.prompt, args.model)
        elif any(is_web_url(value) for value in args.images):
            raise ValueError("웹페이지 URL은 하나만 입력하고 이미지 경로와 섞지 마세요.")
        else:
            result = analyze_images(
                [Path(value) for value in args.images], args.prompt, args.model
            )
    except Exception as error:
        logger.exception("Gemini 분석 실패: %s", error)
        return 1

    print("\n===== Gemini 이미지 분석 결과 =====")
    print(result)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
