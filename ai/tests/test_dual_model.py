import json
from copy import deepcopy
from threading import Barrier
import pytest
from app.services import dual_model as dual


def test_default_dual_models():
    assert dual.FIXED_MODEL == "gemini-3.5-flash-lite"
    assert dual.COST_MODELS["dual35"] == "gemini-3.5-flash"
    assert dual.COST_MODELS["dual36"] == "gemini-3.6-flash"


def sample(schema):
    kind = schema.get("type")
    if kind == "object":
        return {key: sample(value) for key, value in schema["properties"].items()}
    if kind == "array":
        return []
    if isinstance(kind, list):
        return None
    if "enum" in schema:
        return schema["enum"][0]
    return {"boolean": True, "integer": 0, "number": 0.9, "string": ""}[kind]


def test_merge_ownership_and_validation():
    fixed, costs = sample(dual.role_schema("fixed")), sample(dual.role_schema("cost"))
    fixed["property"]["rent"] = 80000
    costs["property"] = {"rent": 1}
    costs["analysis_details"]["validation"]["warnings"] = ["cost warning"]
    costs["analysis_details"]["validation"]["checks"]["evidence_only"] = False
    original = deepcopy(fixed)
    result = dual.merge_results(fixed, costs)
    assert result["property"]["rent"] == 80000
    assert result["analysis_details"]["validation"]["warnings"] == ["cost warning"]
    assert result["analysis_details"]["validation"]["checks"]["evidence_only"] is False
    assert fixed == original
    assert set(result) == set(dual.OUTPUT_SCHEMA["properties"])


def test_parallel_and_postprocess_once(monkeypatch):
    barrier = Barrier(2)
    monkeypatch.setenv("GEMINI_API_KEY", "test")
    def extract(role, *args):
        barrier.wait(timeout=3)
        return sample(dual.role_schema(role)), {"role": role}
    monkeypatch.setattr(dual, "extract", extract)
    calls = []
    def validate(text, *args):
        calls.append(json.loads(text))
        return text
    monkeypatch.setattr(dual, "validate_analysis_json", validate)
    metrics = {}
    dual.analyze(metrics=metrics)
    assert len(calls) == 1
    assert len(metrics["calls"]) == 2


@pytest.mark.parametrize("mode, expected", [("dual35", "gemini-3.5-flash"), ("dual36", "gemini-3.6-flash")])
def test_dual_preset_selects_cost_model(monkeypatch, mode, expected):
    barrier = Barrier(2)
    selected = {}
    monkeypatch.setenv("GEMINI_API_KEY", "test")
    def extract(role, model, *args):
        selected[role] = model
        barrier.wait(timeout=3)
        return sample(dual.role_schema(role)), {"role": role}
    monkeypatch.setattr(dual, "extract", extract)
    monkeypatch.setattr(dual, "validate_analysis_json", lambda text, *args: text)
    dual.analyze(mode=mode)
    assert selected == {"fixed": "gemini-3.5-flash-lite", "cost": expected}


def test_incomplete_half_fails():
    with pytest.raises(RuntimeError, match="Missing field"):
        dual.merge_results(sample(dual.role_schema("fixed")), {})


def test_sdk_usage_aliases_preserve_zero_and_unknown():
    metrics = dual.token_metrics({"total_input_tokens": 123, "total_output_tokens": 42, "total_thought_tokens": 0})
    assert metrics["input_tokens"] == 123
    assert metrics["output_tokens"] == 42
    assert metrics["thoughts_tokens"] == 0
    assert metrics["total_tokens"] is None


def test_failed_half_does_not_return_partial(monkeypatch):
    monkeypatch.setenv("GEMINI_API_KEY", "test")
    def extract(role, *args):
        if role == "cost":
            raise RuntimeError("cost failed")
        return sample(dual.role_schema(role)), {}
    monkeypatch.setattr(dual, "extract", extract)
    with pytest.raises(RuntimeError, match="cost failed"):
        dual.analyze()
