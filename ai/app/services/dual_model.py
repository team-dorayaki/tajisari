"""Split v3.1 extraction; telemetry is deliberately outside the public JSON."""
import json
import logging
import os
import time
from concurrent.futures import ThreadPoolExecutor
from copy import deepcopy

from google import genai
from app.schemas.analysis_output import OUTPUT_SCHEMA, required_object
from gemini_analyze_image import (
    DEFAULT_PROMPT, SCHEMA_GUIDANCE, encode_image, safe_model_dump,
    find_numeric_value, validate_analysis_json, validate_interaction_status,
    validate_url_context_result,
)

logger = logging.getLogger(__name__)
FIXED_MODEL = "gemini-3.5-flash-lite"
COST_MODEL_35 = "gemini-3.5-flash"
COST_MODEL_36 = "gemini-3.6-flash"
COST_MODEL = COST_MODEL_36
COST_MODELS = {"dual": COST_MODEL_36, "dual35": COST_MODEL_35, "dual36": COST_MODEL_36}
FIXED_DETAILS = {"field_analysis", "all_stations", "additional_fields"}
COST_DETAILS = {"cost_candidates", "cost_item_analysis", "reference_information"}


def token_metrics(usage):
    aliases = {
        "input_tokens": "total_input_tokens", "output_tokens": "total_output_tokens",
        "thoughts_tokens": "total_thought_tokens", "tool_use_input_tokens": "total_tool_use_tokens",
        "total_tokens": "total_tokens", "total_cached_tokens": "total_cached_tokens",
    }
    result = {}
    for key, alias in aliases.items():
        value = find_numeric_value(usage, alias)
        result[key] = value if value is not None else find_numeric_value(usage, key)
    return result


def role_schema(role):
    schema = deepcopy(OUTPUT_SCHEMA)
    if role == "single":
        return schema
    props = schema["properties"]
    keep = {"analysis_metadata", "property", "analysis_details"} if role == "fixed" else {"property_cost_items", "analysis_details"}
    props = {key: value for key, value in props.items() if key in keep}
    details = props["analysis_details"]["properties"]
    owned = FIXED_DETAILS if role == "fixed" else COST_DETAILS
    props["analysis_details"] = required_object({key: value for key, value in details.items() if key in owned | {"validation"}})
    return required_object(props)


def validate_shape(value, schema, path="result"):
    kind = schema.get("type")
    if kind == "object":
        if not isinstance(value, dict):
            raise RuntimeError(f"Invalid object: {path}")
        for key in schema.get("required", []):
            if key not in value:
                raise RuntimeError(f"Missing field: {path}.{key}")
        for key, child in schema["properties"].items():
            if key in value:
                validate_shape(value[key], child, f"{path}.{key}")
    elif kind == "array":
        if not isinstance(value, list):
            raise RuntimeError(f"Invalid array: {path}")
        for index, child in enumerate(value):
            validate_shape(child, schema["items"], f"{path}[{index}]")
    else:
        kinds = kind if isinstance(kind, list) else [kind]
        valid = any(
            (item == "null" and value is None)
            or (item == "string" and isinstance(value, str))
            or (item == "boolean" and isinstance(value, bool))
            or (item == "integer" and type(value) is int)
            or (item == "number" and type(value) in (int, float))
            for item in kinds
        )
        if not valid or ("enum" in schema and value not in schema["enum"]):
            raise RuntimeError(f"Invalid value: {path}")


def merge_results(fixed, costs):
    validate_shape(fixed, role_schema("fixed"))
    validate_shape(costs, role_schema("cost"))
    merged = {key: deepcopy(fixed[key]) for key in ("analysis_metadata", "property")}
    merged["analysis_details"] = {key: deepcopy(fixed["analysis_details"][key]) for key in FIXED_DETAILS | {"validation"}}
    merged["property_cost_items"] = deepcopy(costs["property_cost_items"])
    details = merged["analysis_details"]
    for key in COST_DETAILS:
        details[key] = deepcopy(costs["analysis_details"][key])
    left = details["validation"]
    right = costs["analysis_details"]["validation"]
    for key in ("warnings", "conflicts", "unknown_fields"):
        for value in right[key]:
            if value not in left[key]:
                left[key].append(deepcopy(value))
    left["checks"] = {key: value and right["checks"][key] for key, value in left["checks"].items()}
    return merged


def extract(role, model, images, url, prompt):
    schema = role_schema(role)
    scope = {
        "fixed": "property 전체(월세·관리비·보증금·레이킨 포함), all_stations, field_analysis, 비용 외 additional_fields만 추출하세요. validation에는 고정 필드 관련 결과만 기록하세요. 가변 비용 분석은 다른 모델이 담당합니다.",
        "cost": "가변 비용 property_cost_items, cost_candidates, cost_item_analysis 및 비용 관련 validation/reference_information만 추출하세요. 모든 금전 문구를 후보로 수집하며 고정 비용 후보는 PROPERTY로 연결하세요. property 값과 field_analysis는 다른 모델이 담당합니다. 초기/월/연/갱신 보증료를 분리하고 OPTIONAL/UNKNOWN 및 timing을 독립 판단하세요.",
        "single": "전체 v3.1 스키마를 분석하세요.",
    }[role]
    text = f"{prompt}\n{SCHEMA_GUIDANCE}\n이번 요청의 역할(출력 범위 우선): {scope}"
    kwargs = {}
    if url:
        payload = f"{text}\nURL Context로 직접 읽으세요: {url}"
        kwargs["tools"] = [{"type": "url_context"}]
    else:
        payload = [{"type": "text", "text": text + f"\n이미지 수: {len(images)}"}, *images]
    started = time.perf_counter()
    with genai.Client(api_key=os.environ["GEMINI_API_KEY"]) as client:
        response = client.interactions.create(model=model, input=payload,
            generation_config={"thinking_level": "low"},
            response_format={"type": "text", "mime_type": "application/json", "schema": schema}, **kwargs)
    seconds = time.perf_counter() - started
    validate_interaction_status(response)
    if url:
        validate_url_context_result(response)
    data = json.loads(response.output_text)
    validate_shape(data, schema)
    usage = safe_model_dump(getattr(response, "usage", None))
    metrics = {"role": role, "model": model, "api_seconds": seconds, "usage": usage}
    metrics.update(token_metrics(usage))
    return data, metrics


def analyze(image_paths=(), *, url=None, prompt=DEFAULT_PROMPT, mode="dual36", fixed_model=FIXED_MODEL, cost_model=None, single_model=FIXED_MODEL, metrics=None):
    started = time.perf_counter()
    if mode not in {"single", *COST_MODELS}:
        raise ValueError("Analysis mode must be single, dual35, or dual36")
    if not os.getenv("GEMINI_API_KEY"):
        raise RuntimeError("GEMINI_API_KEY 환경 변수가 없습니다.")
    images = [encode_image(path) for path in image_paths]
    if mode in COST_MODELS:
        selected_cost_model = cost_model or COST_MODELS[mode]
        with ThreadPoolExecutor(max_workers=2) as pool:
            fixed = pool.submit(extract, "fixed", fixed_model, images, url, prompt)
            costs = pool.submit(extract, "cost", selected_cost_model, images, url, prompt)
            fixed_data, fixed_metrics = fixed.result()
            cost_data, cost_metrics = costs.result()
        data = merge_results(fixed_data, cost_data)
        calls = [fixed_metrics, cost_metrics]
    else:
        data, call = extract("single", single_model, images, url, prompt)
        calls = [call]
    result = validate_analysis_json(json.dumps(data, ensure_ascii=False), "Gemini", mode,
        "URL" if url else "IMAGE", url, len(images))
    measured = {"mode": mode, "calls": calls, "total_seconds": time.perf_counter() - started}
    logger.info("analysis_metrics=%s", json.dumps(measured, ensure_ascii=False))
    if metrics is not None:
        metrics.update(measured)
    return result
