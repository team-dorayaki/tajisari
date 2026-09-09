"""Compare v3.1 low, v3.1 medium, and dual extraction on identical images."""
import argparse
import json
import subprocess
import time
from types import SimpleNamespace
from unittest.mock import patch
from pathlib import Path
from app.services.dual_model import analyze, token_metrics, safe_model_dump, genai


def single_v31(images, metrics, thinking_level):
    source = subprocess.check_output(["git", "show", "feat/ai-cost-candidates-v3-1:ai/gemini_analyze_image.py"], encoding="utf-8")
    if thinking_level == "medium":
        source = source.replace('"thinking_level": "low"', '"thinking_level": "medium"')
    namespace = {"__name__": "baseline_v31"}
    exec(compile(source, "baseline_v31.py", "exec"), namespace)
    factory = genai.Client
    calls = []
    def client_factory(*args, **kwargs):
        client = factory(*args, **kwargs)
        def create(**request):
            started = time.perf_counter()
            try:
                response = client.interactions.create(**request)
                usage = safe_model_dump(response.usage)
                calls.append({"role": "single", "model": request["model"], "api_seconds": time.perf_counter() - started, "usage": usage, **token_metrics(usage)})
                return response
            finally:
                client.close()
        return SimpleNamespace(interactions=SimpleNamespace(create=create))
    started = time.perf_counter()
    with patch.object(genai, "Client", client_factory):
        result = namespace["analyze_images"](images, namespace["DEFAULT_PROMPT"], "gemini-3.5-flash-lite")
    metrics.update(mode=f"v3.1-{thinking_level}", calls=calls, total_seconds=time.perf_counter() - started)
    return result


def quality(result):
    details = result["analysis_details"]
    costs = result["property_cost_items"]
    names = " ".join(
        str(item.get("raw_name", "")) + " " + str(item.get("display_name", ""))
        + " " + str(item.get("raw_value", "")) for item in costs
    )
    expected = {
        "주차장": ("駐車", "주차"), "화재보험": ("火災", "화재"), "갱신료": ("更新", "갱신"),
        "환경유지비": ("環境", "환경"), "열쇠교환": ("鍵", "열쇠", "Smartlock"),
        "퇴거청소": ("清掃", "청소"), "항균시공": ("抗菌", "항균"),
        "입주자지원": ("入居者", "입주자", "지원 시스템"), "초기보증료": ("初回", "계약 시", "100%"),
        "연간보증료": ("年間", "매년", "2년차"), "인터넷": ("인터넷", "internet"),
    }
    found = {key: any(marker.lower() in names.lower() for marker in markers) for key, markers in expected.items()}
    timings = [{"name": item.get("display_name"), "status": item.get("obligation_status"), "timing": item.get("timing"), "amount": item.get("amount"), "raw": item.get("raw_value")} for item in costs]
    return {
        "expected_costs": len(expected), "found_costs": sum(found.values()),
        "missing_costs": [key for key, value in found.items() if not value],
        "cost_items": len(costs), "cost_candidates": len(details["cost_candidates"]),
        "optional": sum(c["obligation_status"] == "OPTIONAL" for c in costs),
        "unknown": sum(c["obligation_status"] == "UNKNOWN" for c in costs),
        "items": timings, "checks": details["validation"]["checks"],
    }


def fmt(value):
    return "—" if value is None else f"{value:,}" if isinstance(value, int) else str(value)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("images", nargs="+", type=Path)
    parser.add_argument("--output", type=Path, default=Path("benchmark-results"))
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    rows = []
    summaries = []
    runners = (
        ("v3.1-low", lambda metrics: single_v31(args.images, metrics, "low")),
        ("v3.1-medium", lambda metrics: single_v31(args.images, metrics, "medium")),
        ("dual35", lambda metrics: analyze(args.images, mode="dual35", metrics=metrics)),
        ("dual36", lambda metrics: analyze(args.images, mode="dual36", metrics=metrics)),
    )
    for mode, runner in runners:
        metrics = {}
        try:
            result = json.loads(runner(metrics))
            (args.output / f"{mode}.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
            metrics["quality"] = quality(result)
            total_input = sum(call.get("input_tokens") or 0 for call in metrics["calls"])
            total_output = sum(call.get("output_tokens") or 0 for call in metrics["calls"])
            total_thinking = sum(call.get("thoughts_tokens") or 0 for call in metrics["calls"])
            total_tokens = sum(call.get("total_tokens") or 0 for call in metrics["calls"])
            slowest_api = max(call["api_seconds"] for call in metrics["calls"])
            rows.append(f"| {mode} | {len(metrics['calls'])} | {slowest_api:.2f} | {metrics['total_seconds']:.2f} | {total_input:,} | {total_output:,} | {total_thinking:,} | {total_tokens:,} |")
            q = metrics["quality"]
            summaries.append(f"| {mode} | {q['found_costs']}/{q['expected_costs']} | {', '.join(q['missing_costs']) or '없음'} | {q['optional']} | {q['unknown']} | {q['cost_items']} | {q['cost_candidates']} |")
        except Exception as error:
            metrics = {"mode": mode, "error": str(error)}
            rows.append(f"| {mode} | 실패 | — | — | — | — | — | — |")
            summaries.append(f"| {mode} | 실패 | — | — | — | — | — |")
        (args.output / f"{mode}-metrics.json").write_text(json.dumps(metrics, ensure_ascii=False, indent=2), encoding="utf-8")
    report = (
        "# v3.1 low / thinking medium / dual35 / dual36 비교\n\n"
        "공통 입력: `test3_1.png`, `test3_2.png`, `test3_3.png` (각 방식 1회)\n\n"
        "## 시간과 API 사용량\n\n"
        "| 방식 | API 호출 | 병렬 기준 API 시간(초) | 전체 시간(초) | 입력 토큰 | 출력 토큰 | thinking 토큰 | 총 토큰 |\n"
        "|---|---:|---:|---:|---:|---:|---:|---:|\n" + "\n".join(rows) +
        "\n\n## 비용 추출 성능\n\n"
        "이미지에서 확인한 비용 11개(주차장, 화재보험, 갱신료, 환경유지비, 열쇠교환, 퇴거청소, 항균시공, 입주자지원, 초기·연간 보증료, 인터넷)를 기준으로 한다.\n\n"
        "| 방식 | 발견/11 | 누락 | OPTIONAL | UNKNOWN | 최종 비용 행 | 후보 수 |\n"
        "|---|---:|---|---:|---:|---:|---:|\n" + "\n".join(summaries) +
        "\n\n## 상세 판정\n\n각 방식의 금액·의무 상태·timing은 같은 폴더의 `*-metrics.json`에 기록했다. 숫자는 1회 측정이라 평균이나 통계적 우위를 뜻하지 않는다.\n"
    )
    (args.output / "comparison.md").write_text(report, encoding="utf-8")
    print(report)


if __name__ == "__main__":
    main()
