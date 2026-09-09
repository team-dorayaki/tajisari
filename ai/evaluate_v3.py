"""Score repeated v3.0 outputs against visible, source-grounded facts."""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).parent


def contains(items, fragment):
    text = json.dumps(items, ensure_ascii=False)
    return fragment.lower() in text.lower()


def score(path, truth):
    payload = json.loads(path.read_text(encoding="utf-8"))
    result = payload["result"]
    checks = []
    for key, expected in truth["property"].items():
        checks.append((f"property.{key}", result["property"].get(key) == expected))
    for fragment in truth["required_cost_fragments"]:
        checks.append((f"cost:{fragment}", contains(result["property_cost_items"], fragment)))
    facilities = [x for x in result["analysis_details"]["additional_fields"] if x.get("category") == "FACILITY"]
    for fragment in truth["required_facility_fragments"]:
        checks.append((f"facility:{fragment}", contains(facilities, fragment)))
    checks.append(("schema_version", result["analysis_metadata"].get("schema_version") == "3.0"))
    checks.append(("no_v31_candidates", "cost_candidates" not in result["analysis_details"]))
    passed = sum(ok for _, ok in checks)
    return {"file": path.name, "passed": passed, "total": len(checks), "failed": [name for name, ok in checks if not ok]}


parser = argparse.ArgumentParser()
parser.add_argument("case", choices=["images", "url"])
parser.add_argument("files", nargs="+")
args = parser.parse_args()
truth = json.loads((ROOT / "evaluation/ground_truth.json").read_text(encoding="utf-8"))[args.case]
reports = [score(Path(file), truth) for file in args.files]
values = []
for report in reports:
    report["ratio"] = round(report["passed"] / report["total"], 4)
    values.append(report["ratio"])
payloads = [json.loads(Path(file).read_text(encoding="utf-8"))["result"] for file in args.files]
property_keys = set().union(*(payload["property"] for payload in payloads))
inconsistent = [
    key for key in sorted(property_keys)
    if len({json.dumps(payload["property"].get(key), ensure_ascii=False, sort_keys=True) for payload in payloads}) > 1
]
summary = {
    "case": args.case,
    "runs": reports,
    "mean_ratio": round(sum(values) / len(values), 4),
    "min_ratio": min(values),
    "inconsistent_property_fields": inconsistent,
}
(ROOT / f"evaluation/{args.case}-report.json").write_text(
    json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8"
)
print(json.dumps(summary, ensure_ascii=False, indent=2))
