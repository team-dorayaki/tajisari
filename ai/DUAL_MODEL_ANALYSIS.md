# AI 분석 v3.1: 이중 모델

Python API 서버는 두 dual 프리셋을 제공한다. `dual35`는 고정 필드 `gemini-3.5-flash-lite` + 가변 비용 `gemini-3.5-flash`, 기본값인 `dual36`은 고정 필드 `gemini-3.5-flash-lite` + 가변 비용 `gemini-3.6-flash`다. 이미지를 한 번 인코딩하고 두 요청을 ThreadPoolExecutor로 병렬 실행한다. URL도 두 URL Context 요청으로 병렬 처리한다.

`AI_ANALYSIS_MODE`는 `single`, `dual35`, `dual36` 중 하나를 선택한다. `GEMINI_MODEL`은 단일 모델, `GEMINI_FIXED_MODEL`은 고정 모델을 설정하며 `GEMINI_COST_MODEL`을 지정하면 프리셋의 비용 모델만 재정의한다. 두 모델 모두 `thinking_level="low"`를 명시한다.

## 병합 계약

| 영역 | 소유 모델 |
|---|---|
| property 전체, all_stations, field_analysis | Flash Lite |
| 비용 외 additional_fields, analysis_metadata | Flash Lite |
| property_cost_items, cost_candidates, cost_item_analysis | Flash |
| 비용 reference_information | Flash |
| validation | 각 모델이 자기 영역만 출력, warnings/conflicts/unknown_fields를 중복 없이 결합, checks는 AND |

스키마의 실제 키는 `reference_information`이다. 비용 모델은 월세 등 고정 금액을 property에 덮어쓰지 않으며 PROPERTY 비용 후보로만 기록한다. 비용 배열 인덱스는 그대로 병합한 후 기존 `validate_analysis_json`을 한 번 호출하여 v3.1 후처리를 적용한다. 한 모델 실패·잘못된 구조는 전체 요청 실패로 처리한다. 자동 재호출이나 단일 모델 대체는 하지 않는다.

최종 result의 v3.1 구조는 그대로다. API 외부 envelope의 model 문자열은 두 모델명을 `+`로 표시한다. 시간과 토큰은 `analysis_metrics` 로그와 선택적 호출자 metrics 딕셔너리에 기록하며 result에는 추가하지 않는다. SDK usage 원본을 함께 보존하고 total_input_tokens/total_output_tokens/total_thought_tokens 및 이전 이름을 지원한다. 미제공 값은 null, 실제 0은 0으로 보존한다.

## 실제 비교 (2026-09-09)

아래 결과는 비용 모델을 3.6으로 변경하기 전에 `gemini-3.5-flash`로 측정한 과거 기준값이다.

동일한 test1_1.png~test1_3.png 3장을 사용했다. 기준 브랜치 `feat/ai-cost-candidates-v3-1`의 원본 analyze_images를 git show로 읽어 실행했다. 기준은 Flash Lite, thinking low이고 이중 모델도 low다. 각 방식 1회인 탐색 측정이므로 평균 성능이나 정확도 우위를 의미하지 않는다.

| 방식 / 역할 | API 응답(초) | 입력 토큰 | 출력 토큰 | thinking 토큰 | 전체 분석(초) |
|---|---:|---:|---:|---:|---:|
| 원본 단일 Flash Lite | 13.90 | 5,391 | 4,315 | 0 | 14.07 |
| 이중 / 고정 Flash Lite | 11.24 | 5,502 | 2,975 | 0 | — |
| 이중 / 비용 Flash | 22.94 | 5,534 | 3,546 | 0 | — |
| 이중 합계 | 병렬 실행 | 11,036 | 6,521 | 0 | 22.95 |

API 호출 수는 1→2, 공급자 total_tokens는 9,706→17,557(+80.9%)다. 입력은 +104.7%, 출력은 +51.1%, 전체 분석 시간은 +63.1%다. 두 API 시간 합계 34.18초보다 전체 22.95초가 짧아 병렬 실행을 확인했다. 전체 분석 시간에는 인코딩·API·병합·후처리가 포함되고 HTTP 업로드 시간은 제외된다. 모델별 단가가 다르므로 토큰 증가율은 금액 증가율이 아니다. SDK가 보고한 thinking은 모두 0이었다.

| 검증 항목 | 원본 단일 | 이중 모델 |
|---|---|---|
| 가변 비용 행 | 4 | 7 |
| 초기 비용 30,000 / 월 서포트 1,500 / 퇴거 수수료 15,000 | 모두 추출 | 모두 추출 |
| 보증료 시점 분리 | 초기·연간·월간을 한 행 RENEWAL로 결합 | INITIAL / RENEWAL / MONTHLY 3행 분리 |
| 보증료 비율·범위 | 금액 null | 초기 비율·월 범위 null, 연간 10,000 |
| 보험 불필요 문구 | 비용 후보 누락 | 0원으로 보존 |
| OPTIONAL/UNKNOWN | OPTIONAL 0, UNKNOWN 3 | 보험 OPTIONAL, 기타 초기·서포트·연간·월 보증료 UNKNOWN |
| timing | 보증료 결합 오류 | 보증료 개선, 보험 INITIAL은 시점 근거 없어 오류 |
| 고정 필드 병합 | 해당 없음 | 월세 44,800, 관리비 15,000, 면적 7, 도보 5, 계약 12개월 유지 |

보험의 '필요없음'을 OPTIONAL로 분류한 것은 명시적 선택 가입 근거가 없으므로 UNKNOWN이 더 보수적이다. 첫 탐색 실행에서는 연간/월 보증료의 대체 관계를 OPTIONAL로 오판하기도 했다. 기존 v3.1 후처리는 REQUIRED 근거는 검사하지만 OPTIONAL과 모든 timing 오판을 교정하지 못한다. 이번 구현에서는 기존 후처리 정책을 변경하지 않았다. 추가 샘플과 명시적 OPTIONAL 사례로 정확도를 검증할 필요가 있다. 비용 누락의 전수 보장을 뜻하지 않으며 cost_candidates_classified=true만으로 누락 없음이 증명되지는 않는다.

## 검증과 재실행

`python -m pytest tests -q` (ai 디렉터리): 총 47개 통과. 병렬 진입, 후처리 한 번 실행, 고정 필드 소유권, validation 병합, 실패한 절반의 부분 반환 방지, SDK usage 호환성과 규칙 기반 교차 검증을 검사한다.

`python benchmark_dual.py test1_1.png test1_2.png test1_3.png --output benchmark-results/baseline-comparison`

실제 JSON과 토큰 원본은 로컬 benchmark-results/baseline-comparison에 있다. benchmark-results와 test*.png는 Git ignore 대상이다. 테스트 이미지는 커밋에 포함하지 않는다.
