import argparse
import base64
import json
import mimetypes
import os
import sys
import time
from pathlib import Path
from urllib.parse import urlparse

from google import genai


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
    print(
        f"[전송] URL Context 요청, 프롬프트 {len(request_text):,}자",
        file=sys.stderr,
    )
    client = genai.Client(api_key=api_key)
    api_started = time.perf_counter()
    interaction = client.interactions.create(
        model=model,
        input=request_text,
        tools=[{"type": "url_context"}],
        generation_config={"thinking_level": "low"},
        response_format={"type": "text", "mime_type": "application/json", "schema": OUTPUT_SCHEMA},
    )
    print(
        f"[시간] URL Context + Gemini API 응답: "
        f"{time.perf_counter() - api_started:.2f}초",
        file=sys.stderr,
    )
    try:
        print_url_context_status(interaction, requested_url=url)
    except Exception as log_error:
        print(f"[URL Context] 로그 해석 실패: {log_error}", file=sys.stderr)
    if not interaction.output_text:
        raise RuntimeError("Gemini 응답에 분석 결과가 없습니다.")
    result = validate_analysis_json(
        interaction.output_text,
        provider="Gemini",
        debug_info=f"status={getattr(interaction, 'status', None)}",
    )
    print(f"[시간] 전체 처리: {time.perf_counter() - total_started:.2f}초", file=sys.stderr)
    return result


def print_url_context_status(interaction, requested_url: str) -> None:
    """URL 조회 여부, 상태, 인용 및 도구 토큰을 진단 가능한 형태로 출력한다."""
    steps = getattr(interaction, "steps", []) or []
    context_steps = [
        step for step in steps if getattr(step, "type", None) == "url_context_result"
    ]
    print(
        f"[URL Context] tool_called={str(bool(context_steps)).lower()}, "
        f"requested_url={requested_url}",
        file=sys.stderr,
    )

    status_values = []
    retrieved_urls = []
    serialized_steps = []
    for step in context_steps:
        payload = safe_model_dump(step)
        serialized_steps.append(payload)
        collect_url_context_values(payload, status_values, retrieved_urls)

    if context_steps:
        print(
            f"[URL Context] status={', '.join(dict.fromkeys(status_values)) or 'unknown'}",
            file=sys.stderr,
        )
        if retrieved_urls:
            print(
                "[URL Context] retrieved_url="
                + ", ".join(dict.fromkeys(retrieved_urls)),
                file=sys.stderr,
            )
        if not status_values or not retrieved_urls:
            print(
                "[URL Context 상세] "
                + json.dumps(serialized_steps, ensure_ascii=False),
                file=sys.stderr,
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
    print(f"[URL Context] citation_count={len(citation_urls)}", file=sys.stderr)
    for citation_url in dict.fromkeys(citation_urls):
        print(f"[URL Context] citation_url={citation_url}", file=sys.stderr)

    usage = getattr(interaction, "usage", None)
    usage_payload = safe_model_dump(usage)
    tool_tokens = find_numeric_value(usage_payload, "tool_use_input_tokens")
    total_tokens = find_numeric_value(usage_payload, "total_tokens")
    print(
        f"[URL Context] tool_use_input_tokens={tool_tokens if tool_tokens is not None else 'unknown'}, "
        f"total_tokens={total_tokens if total_tokens is not None else 'unknown'}",
        file=sys.stderr,
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
    print(f"[시간] 이미지 전처리: {preprocess_seconds:.2f}초", file=sys.stderr)

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
    api_seconds = time.perf_counter() - api_started
    print(f"[시간] Gemini API 응답: {api_seconds:.2f}초", file=sys.stderr)

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
    )
    validation_seconds = time.perf_counter() - validation_started
    total_seconds = time.perf_counter() - total_started
    print(f"[시간] JSON 검증: {validation_seconds:.2f}초", file=sys.stderr)
    print(f"[시간] 전체 처리: {total_seconds:.2f}초", file=sys.stderr)
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


def apply_semantic_checks(data: dict) -> None:
    """모델 응답의 계산값, 출처와 영역 간 관계를 결정적으로 검증한다."""
    assessment = data.get("assessment", {})
    warnings = assessment.setdefault("warnings", [])
    issues = []

    location = data.get("location", {})
    agency = data.get("agency", {})
    if location.get("postal_code") and location.get("postal_code") == agency.get("postal_code"):
        location["postal_code"] = None
        issues.append("매물과 회사 우편번호가 같아 매물 우편번호를 null로 보정했습니다.")

    metadata = data.get("analysis_metadata", {})
    room_number = str(data.get("property", {}).get("room_number") or "")
    floor = str(data.get("property", {}).get("floor") or "")
    conflicts = metadata.get("conflicts")
    if room_number and floor and isinstance(conflicts, list):
        filtered = [
            conflict for conflict in conflicts
            if not (room_number in str(conflict) and floor in str(conflict))
        ]
        if len(filtered) != len(conflicts):
            metadata["conflicts"] = filtered
            issues.append("호실 번호와 실제 층수를 충돌로 본 항목을 제거했습니다.")
    if metadata.get("conflicts") and metadata.get("cross_image_consistency") == "consistent":
        metadata["cross_image_consistency"] = "conflict"
        issues.append("conflicts가 존재하여 cross_image_consistency를 conflict로 보정했습니다.")

    pricing = data.get("pricing", {})
    fixed_amounts = []
    fixed_known = True
    for key in ("rent", "management_fee", "common_service_fee"):
        cost = pricing.get(key)
        amount = cost.get("amount_yen") if isinstance(cost, dict) else None
        if amount is None:
            if key == "common_service_fee":
                amount = 0
            else:
                fixed_known = False
        if isinstance(amount, int):
            fixed_amounts.append(amount)
    if fixed_known:
        calculated_total = sum(fixed_amounts)
        if pricing.get("base_monthly_total_yen") != calculated_total:
            pricing["base_monthly_total_yen"] = calculated_total
            issues.append("월세·관리비·공익비로 base_monthly_total_yen을 다시 계산했습니다.")

    variable_costs = pricing.setdefault("variable_monthly_costs", [])
    for term in data.get("contract", {}).get("guarantee_fee_terms", []):
        frequency = str(term.get("frequency") or "").lower()
        if ("월" in frequency or "month" in frequency) and term not in variable_costs:
            variable_costs.append(term.copy())
            issues.append("월 단위 보증료를 variable_monthly_costs에 반영했습니다.")

    evidence_fields = {
        item.get("field") for item in data.get("source_evidence", [])
        if isinstance(item, dict)
    }
    required_evidence = {
        "property.property_name_jp",
        "location.address_raw",
        "pricing.rent.amount_yen",
        "pricing.management_fee.amount_yen",
    }
    missing_evidence = sorted(required_evidence - evidence_fields)
    if missing_evidence:
        issues.append("핵심 값의 source_evidence 누락: " + ", ".join(missing_evidence))

    if issues:
        warnings.extend(issue for issue in issues if issue not in warnings)
        confidence = assessment.get("confidence")
        if isinstance(confidence, dict) and confidence.get("overall") == "high":
            confidence["overall"] = "medium"


def validate_analysis_json(result: str, provider: str, debug_info: str) -> str:
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

    apply_semantic_checks(data)

    return json.dumps(data, ensure_ascii=False, indent=2)


def main() -> int:
    args = parse_args()

    try:
        if len(args.images) == 1 and is_web_url(args.images[0]):
            print(f"웹페이지 분석 중: {args.images[0]}", file=sys.stderr)
            result = analyze_webpage(args.images[0], args.prompt, args.model)
        elif any(is_web_url(value) for value in args.images):
            raise ValueError("웹페이지 URL은 하나만 입력하고 이미지 경로와 섞지 마세요.")
        else:
            result = analyze_images(
                [Path(value) for value in args.images], args.prompt, args.model
            )
    except Exception as error:
        print(f"오류: {error}", file=sys.stderr)
        return 1

    print("\n===== Gemini 이미지 분석 결과 =====")
    print(result)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
