# thinking_level low → medium 비교

실행일: 2026-09-09. 기준: `feat/ai-cost-candidates-v3-1` (`a56605c90ec695ce83ade0cd40766f31537f0260`). 작업 브랜치: `feat/ai-thinking-medium`.

## 구현 및 측정 조건

- `ai/gemini_analyze_image.py`의 이미지·URL 요청 두 곳만 `thinking_level="medium"`으로 변경했다. 모델은 `gemini-3.5-flash-lite`로 유지했다.
- v3.1 스키마, 프롬프트, JSON 검증 및 비용 후처리는 변경하지 않았다. 기준 소스에 두 문자열 치환만 적용한 결과와 작업 소스가 동일함을 실행 전 assertion으로 확인했다.
- `test1_1.png`~`test1_3.png`, `test2_1.png`~`test2_3.png`, `test3_1.png`~`test3_3.png`: 각 매물의 3장을 한 요청에 넣었다. 각 묶음 low→medium 순서로 각 1회, 총 6회 호출했다. 이미지 바이트의 SHA-256은 원시 결과 JSON에 기록했다. 이미지 파일은 Git에 추가하거나 커밋하지 않았다.
- 기존 `analyze_images` 함수를 직접 실행했다. 전체 시간은 클라이언트 생성·이미지 읽기/base64·API·검증·후처리를 포함하며, HTTP 업로드/프런트엔드/백엔드 네트워크 시간은 포함하지 않는다. URL은 설정 변경만 확인했고 실서비스 호출 비교는 이미지에 한정했다.
- 토큰은 API usage의 `total_input_tokens`, `total_output_tokens`, `total_thought_tokens`를 사용했다. 기존 로그 함수가 찾는 키와 실제 SDK 키가 달라, 벤치마크에서 usage 원본을 별도로 수집했다. 서비스 로그 코드는 변경하지 않았다. 캐시 토큰은 6회 모두 0이었다.

## 성능·토큰

| 이미지 묶음 | thinking | Gemini API(초) | 전체 처리(초) | 입력 토큰 | 출력 토큰 | thinking 토큰 | 총 토큰 |
|---|---|---:|---:|---:|---:|---:|---:|
| test1, 3장 | low | 16.62 | 17.26 | 5,391 | 4,874 | 0 | 10,265 |
| test1, 3장 | medium | 25.95 | 26.60 | 5,391 | 5,974 | 2,697 | 14,062 |
| test2, 3장 | low | 17.13 | 17.66 | 5,399 | 5,758 | 0 | 11,157 |
| test2, 3장 | medium | 25.80 | 26.50 | 5,399 | 6,361 | 2,614 | 14,374 |
| test3, 3장 | low | 18.89 | 19.45 | 5,399 | 6,340 | 0 | 11,739 |
| test3, 3장 | medium | 28.40 | 28.97 | 5,399 | 6,992 | 3,275 | 15,666 |
| 평균 | low | 17.55 | 18.12 | 5,396.33 | 5,657.33 | 0 | 11,053.67 |
| 평균 | medium | 26.72 | 27.36 | 5,396.33 | 6,442.33 | 2,862 | 14,700.67 |

평균 API 시간은 약 52.3%, 전체 처리 시간은 약 51.0%, 총 토큰은 약 33.0% 증가했다. 각 조건 1회 측정으로 분산·유의성이나 지속적인 품질 향상을 판단할 수 없다.

## 이미지 원문 대조

가변 비용 누락은 이미지에 보이는 양수 금액·비율·범위 비용을 시점별로 나눠 대조했다. 월세·관리비·시키킨·레이킨과 무료/불필요 문구는 이 개수에서 제외했다. 후보 수 자체를 정확도 지표로 사용하지 않았다.

| 검증 항목 | low | medium | 판단 |
|---|---|---|---|
| test1 가변 비용 | 6/6 추출 | 6/6 추출 | 초기비용, 안심서포트, 해약수수료, 최초·연간·월간 보증료 모두 존재 |
| test2 가변 비용 | 9/9 추출 | 9/9 추출 | 사무·제균·침구·수도광열·가구철거·단기위약금·보증료 3개 모두 존재 |
| test3 가변 비용 | 11/11 추출 | 10/11 추출 | medium은 월정액 주차장 ¥4,400~ 누락. cost_candidates에도 없음 |
| 명시적 선택 비용 | test2 3개, test3 2개 OPTIONAL | 동일한 5개 OPTIONAL | 침구·수도광열·가구철거·항균시공·입주자지원 모두 유지 |
| UNKNOWN 개수 | test1 5, test2 6, test3 9 | test1 5, test2 6, test3 8 | test3 감소는 주차장 누락 때문이며 개선이 아님 |
| test1 의무 판정 | 해약수수료 REQUIRED, 최초 보증료 UNKNOWN | 해약수수료 UNKNOWN, 최초 보증료 REQUIRED | 개수는 같지만 대상이 바뀜. 가입 필수·퇴거 시 발생 원문과 추가 검토 필요 |
| test2 단기위약금 timing | 최종 MONTHLY | 최종 MONTHLY | 월액 합계 1개월분은 산정 근거이며 매월 납부가 아님. 두 설정 모두 오류 |
| test2 침구 timing | UNKNOWN | INITIAL | 원문은 任意와 금액만 명시. medium이 근거 없는 시점을 부여 |
| test2 가구철거 timing | MOVE_OUT | MOVE_OUT | 원문에 퇴거 시점 명시가 없어 두 설정 모두 추정 |
| test3 주요 timing | 갱신 RENEWAL, 환경유지·인터넷 MONTHLY, 청소 MOVE_OUT | 동일 | 명시된 시점 보존 |
| test3 미기재 timing | 열쇠·보험·항균·지원 INITIAL | 동일 | 초기 납부 시점이 직접 명시되지 않아 UNKNOWN이 더 보수적 |
| 보증료 시점 분리 | test1/2 각 3행, test3 2행 | 동일 | INITIAL/RENEWAL/MONTHLY 및 INITIAL/RENEWAL로 각각 분리 |
| test1 연간/월간 대체 보증 플랜 | 후보 적용 YES/YES | CONDITIONAL/CONDITIONAL | medium은 대체 조건을 더 명시적으로 반영. 둘 다 의무 UNKNOWN |
| test1 해약수수료 금액 | 15,000 | null | medium은 명시된 세전 금액을 보존하지 못함 |
| test3 주차장 범위 금액 | 4,400으로 확정 | 항목 누락 | low도 범위 표현을 null로 보존하는 규칙 위반 |

test2 medium 원시 응답의 단기위약금 timing은 CONDITIONAL이었지만 기존 `infer_cost_timing`이 `月額合計`에 반응해 MONTHLY로 바꿨다. low에서는 모델이 후보만 반환한 위약금을 후처리가 복구하면서 MONTHLY로 분류했다. 후처리를 그대로 유지하는 이번 변경만으로 해결되지 않는다.

## 검증 및 작업 폴더 상태

- 기존 AI 테스트: `.venv-new/Scripts/python.exe -B -m pytest ai/tests -q -p no:cacheprovider` → **18 passed**, 의존성 deprecation warning 2개.
- 실 API 6회 모두 JSON 검증·후처리 완료, schema_version 3.1 유지.
- 대상 분석 파일의 `git diff --check` 통과. 프롬프트·출력 스키마 파일 diff 없음.
- 작업 도중 다른 작업에서 `ai/app/core/config.py`, `ai/app/services/gemini_analyzer.py`, 라우트, 환경 예시, `.gitignore`, 신규 `dual_model.py`가 수정됨을 발견했다. 이 파일은 이 작업에서 수정하지 않았다. 현재 FastAPI 기본 경로에는 별도 dual-model 변경이 섞여 있으므로, 이 보고서는 기준 v3.1 단일 모델 함수 비교이며 현재 공유 폴더 전체가 단일 모델 상태라는 검증은 아니다. 전체 diff의 공백 경고도 해당 별도 변경에서 발생했다.
- 커밋은 생성하지 않았다. 테스트 이미지는 staging하지 않았다.

요청한 medium 설정은 구현했지만, 이번 표본은 지연과 토큰 증가에 비해 일관된 품질 개선을 보여주지 않았다. 특히 주차비 누락과 기존 timing 오류를 결과에 남긴다.

원시 결과 위치: `C:\Users\poptr\OneDrive\문서\ChatGPT\ai공부\thinking_benchmark_results`. 파일: `test1_low.json`, `test1_medium.json`, `test2_low.json`, `test2_medium.json`, `test3_low.json`, `test3_medium.json`. 각 파일에 이미지 해시, API usage, 원시 응답, 후처리 응답, 시간 로그가 있다.
