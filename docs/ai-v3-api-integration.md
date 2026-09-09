# AI v3.0 integration — 2026-09-09

## Git evidence

Worktree: `.worktrees/ai-v3-api-integration`, branch `feat/ai-v3-api-integration`, base `origin/dev` at `ec41e78`. Original checkout and its uncommitted changes are preserved.

Historical v3.0: `a5f9bc4` (`feat/ai-analysis-schema-v3`), also integrated as `0195556`.
Sources: `ai/app/schemas/analysis_output.py`, `ai/app/prompts/image_analysis.py`, `ai/app/services/gemini_analyzer.py`, `ai/gemini_analyze_image.py`, `ai/app/core/config.py`.
Historical service invokes `analyze_images` or `analyze_webpage` with one model. No fixed/cost model split exists there.

| Field/config | Historical v3.0 | Current dev v3.1 |
|---|---|---|
| analysis_metadata.schema_version | 3.0 | 3.1 |
| analysis_details.cost_candidates | absent | required array |
| analysis_details.validation.checks.cost_candidates_classified | absent | required boolean |
| Remaining schema fields | identical | identical |
| API model flow | one model | fixed/cost split |

The historical schema is copied exactly to `ai/app/schemas/analysis_output_v3.py`. The API calls one model; current candidate extraction and semantic checks stay internal. The response mapper removes the two v3.1-only fields, sets version 3.0, and validates against the historical schema. This is not a version-string-only downgrade.

## API contract

The dev backend already exposes POST `/api/property-analyses/images` (multipart `files`) and POST `/api/property-analyses/url` (JSON `url`). It uses `ApiResponse` around `PropertyAnalysisResponse`. The Python service and backend client now use `/api/property-analyses/images` and `/api/property-analyses/url` as well. The original checkout received the same targeted path changes. Live port 8000 OpenAPI was verified to expose the new paths.

Notion project specification could not be found in the connected workspace. A link has been requested; final external envelope/error contract verification is pending. The live tests below exercise Python API endpoints, not authenticated Spring backend endpoints.

## Validation and first results

52 pytest tests passed. Both supplied image and URL cases returned HTTP 200 and passed historical v3.0 schema validation using `gemini-3.5-flash-lite`. Each request uses one model invocation. Original results are retained in `ai/results/images-first.json` and `ai/results/url-first.json`; later runs are in `images-improved.json` and `url-improved.json`.

Image first result: rent 32000, management fee 4000, area 26.49, deposit 0, available date 2026-09-15. Key money is null because the source states one month, and v3.0 forbids calculating its yen amount. Bus plus walk is not represented as direct station walking time.

URL model result: Comfort Luminous, rent 86600, management fee 4000, area 56.89. URL Context reported successful retrieval. Independent web fetch failed, so this is not independently verified ground truth.

## Improvements and remaining issues

1. Recovered cost candidates now keep explicit OPTIONAL/REQUIRED evidence instead of always becoming UNKNOWN. Regression test passed; image rerun marks the two optional services OPTIONAL.
2. Prompt now preserves raw language, checks non-cost rows, and separates bus access from direct station walking. Rerun preserves Korean cost names and includes structure/floor/year/facility information.
3. Slash-separated period suffixes are attached to the preceding fee rather than becoming standalone costs. Regression test passed; URL rerun has two guarantee fee rows instead of three.

Accuracy is not complete: image rerun omitted the parking price previously found, still groups facilities, and omits other descriptive fields. URL output has a spaced `10 000円` amount that is not normalized and insurance extraction varies between runs. These require further single-model extraction/validation work; current outputs must not be described as fully accurate.

## Finite evaluation loop

`ai/evaluation/ground_truth.json` contains only facts visible in the supplied sources. `ai/evaluate_v3.py` scores required property values, cost coverage, facility coverage, v3.0 shape, and cross-run property consistency. The loop is capped at three runs per input and five correction cycles.

With the same code, three URL runs scored 100%/100%/100%. Three low-thinking image runs scored 100%/100%/74.1%; the weak run collapsed facility detail to ` 외에는 모두 있음`. Medium thinking regressed to 71.4%/67.9%/71.4% and was rejected. Generic near-white margin trimming then produced 96.4%/92.9%/100%, raising the image minimum from 74.1% to 92.9%. The remaining repeated errors were one missed environment-maintenance fee and two unsupported key-money zeros. Duplicate unlabeled evidence for deposit and key money is now conservatively rejected, with a regression test. Further API repetition is stopped until a new general correction hypothesis exists.

After evidence-first zero validation was added, one final image run scored 28/28 (100%): `ai/results/images-final.json`. The suite now has 57 passing tests. This final single success confirms the correction path but does not replace the three-run minimum of 92.9% as the honest consistency measure.

Reproduce from `ai`: `../.venv/Scripts/python.exe run_v3_smoke.py images --label next` or `url --label next`. API key comes from the environment, never from committed files.
