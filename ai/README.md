# Gemini 일본 부동산 분석 API

Gemini 3.5 Flash-Lite로 일본 부동산 매물 이미지 또는 공개 웹페이지 URL을 분석하는 FastAPI 서버입니다.

## 구조

```text
ai/
├─ app/
│  ├─ api/routes/
│  │  ├─ health.py
│  │  └─ image_analysis.py
│  ├─ core/config.py
│  ├─ schemas/image.py
│  ├─ services/gemini_analyzer.py
│  └─ main.py
├─ tests/test_health.py
├─ gemini_analyze_image.py
├─ requirements.txt
└─ Dockerfile
```

`gemini_analyze_image.py`는 기존 Gemini 분석 엔진과 이전 CLI 호환용으로 유지합니다. REST API는 `app` 아래의 라우터와 서비스를 통해 이 엔진을 호출합니다.

## 로컬 개발 환경 준비

### 1. 준비 사항

- Python 3.12 또는 3.13
- Google AI Studio에서 발급한 Gemini API 키
- Windows PowerShell

### 2. 프로젝트 폴더로 이동

아래 경로는 예시입니다. 자신의 프로젝트 경로에 맞게 변경합니다.

```powershell
cd C:\Users\사용자명\IdeaProjects\kb-Hackerton
```

### 3. 가상환경 생성 및 활성화

프로젝트를 처음 실행한다면 프로젝트 루트에서 가상환경을 생성합니다.

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
```

정상적으로 활성화되면 PowerShell 명령줄 앞에 `(.venv)`가 표시됩니다.

이미 `.venv` 가상환경이 만들어져 있다면 생성 명령은 생략하고 활성화만 합니다.

```powershell
.\.venv\Scripts\Activate.ps1
```

PowerShell 실행 정책 때문에 활성화가 차단되면 현재 터미널에서만 실행 정책을 변경한 뒤 다시 시도합니다.

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\.venv\Scripts\Activate.ps1
```

### 4. 패키지 설치

가상환경을 활성화한 상태에서 `ai` 폴더로 이동하고 필요한 패키지를 설치합니다.

```powershell
cd ai
python -m pip install -r requirements.txt
```

### 5. Gemini API 키 설정

API 키는 코드에 직접 작성하지 않고 환경변수로 등록합니다. 다음 설정은 현재 PowerShell 창에서만 유지됩니다.

```powershell
$env:GEMINI_API_KEY="본인의_API_KEY"
```

키가 설정됐는지는 실제 키를 출력하지 않고 다음처럼 확인할 수 있습니다.

```powershell
if ($env:GEMINI_API_KEY) { "API 키 설정됨" } else { "API 키 없음" }
```

## 서버 실행

가상환경이 활성화되어 있고 현재 위치가 `ai` 폴더인지 확인한 뒤 서버를 실행합니다.

```powershell
python -m uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```

`Application startup complete.`가 출력되면 서버가 정상적으로 실행된 것입니다. 서버를 종료할 때는 실행 중인 PowerShell에서 `Ctrl+C`를 누릅니다.

확인 주소:

- Swagger UI: http://127.0.0.1:8000/docs
- 상태 확인: http://127.0.0.1:8000/health

## 로그

서버 로그는 터미널과 `ai/logs` 폴더에 함께 기록됩니다.

- `logs/app.log`: Gemini 처리 시간, URL Context 상태, 토큰 및 캐시 사용량
- `logs/access.log`: 요청 ID, HTTP 경로, 상태 코드 및 처리 시간
- `logs/error.log`: 처리 중 발생한 오류와 스택 추적

로그 파일은 파일당 최대 10MB이며 이전 로그를 최대 5개까지 보관합니다. API 키, 이미지 Base64 데이터와 전체 분석 결과는 기록하지 않습니다. 로그 파일은 Git에서 제외되고 `logs/.gitkeep`만 저장소에 포함됩니다.

각 HTTP 응답의 `X-Request-ID` 헤더를 이용하면 같은 요청에서 발생한 로그를 찾을 수 있습니다.

## 이미지 분석

`POST /api/v1/analysis/images`에 `multipart/form-data`로 `files`를 여러 번 전달합니다.

```powershell
curl.exe -X POST "http://127.0.0.1:8000/api/v1/analysis/images" `
  -F "files=@test_jp_img4.png" `
  -F "files=@test_jp_img5.png"
```

## URL 분석

`POST /api/v1/analysis/url`에 JSON을 전달합니다. Gemini의 URL Context가 공개 페이지를 직접 조회합니다.

```powershell
$body = @{
  url = "https://www.best-estate.jp/ko/property/2178457/"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://127.0.0.1:8000/api/v1/analysis/url" `
  -ContentType "application/json" `
  -Body $body
```

## 응답 형식

분석 결과는 DB에 바로 대응하는 `property`, `property_cost_items`와 검증용 `analysis_details`로 구분합니다. 전체 결과는 `property_ai_analysis.raw_json`에 보존할 수 있습니다. 아래 JSON은 일부 필드를 생략한 예시입니다.

```json
{
  "input_type": "images",
  "model": "gemini-3.5-flash-lite",
  "result": {
    "analysis_metadata": {
      "schema_version": "3.0",
      "source_type": "IMAGE",
      "image_count": 2
    },
    "property": {
      "source_site": "SUUMO",
      "source_url": null,
      "property_name": "サンプルハイツ 203",
      "prefecture": "東京都",
      "city": "北区",
      "exclusive_area_m2": 18.0,
      "nearest_station": "栄町駅",
      "walk_minutes": 5,
      "rent": 65000,
      "management_fee": 5000,
      "deposit": 65000,
      "key_money": 65000,
      "available_from": null,
      "contract_period_months": null,
      "listed_initial_cost_total": null
    },
    "property_cost_items": [
      {
        "raw_name": "保証委託料",
        "display_name": "보증 위탁료",
        "amount": null,
        "raw_value": "賃料総額の50%",
        "obligation_status": "REQUIRED",
        "timing": "INITIAL"
      }
    ],
    "analysis_details": {
      "field_analysis": [],
      "cost_item_analysis": [],
      "all_stations": [],
      "additional_fields": [],
      "reference_information": [],
      "validation": {
        "conflicts": [],
        "warnings": [],
        "unknown_fields": [],
        "checks": {
          "evidence_only": true,
          "duplicates_removed": true,
          "conflicts_reviewed": true,
          "fixed_costs_not_duplicated": true,
          "listing_terms_preferred": true,
          "amounts_match_raw_text": true,
          "required_status_has_evidence": true
        }
      }
    }
  }
}
```

DB 대응 필드가 `null`이면 미기재 또는 미확인이고, 원문에 `なし`, `不要`, `0円`이 명시된 경우에만 `0`으로 기록합니다. 신뢰도와 근거는 `analysis_details`에 분리되며, 신뢰도가 0.7 미만이거나 근거가 없거나 충돌이 해결되지 않으면 `needs_review`가 `true`가 됩니다.

Gemini는 원문 추출만 담당하며 비율·배수·합산·기간 환산을 수행하지 않습니다. `1ヶ月`, `総賃料の50%`, `2年`처럼 계산이 필요한 표현은 원문으로 보존하고 계산 결과 필드는 `null`로 반환합니다. 대표 역은 도보 시간이 가장 짧은 한 곳만 `property`에 두고 전체 역은 `analysis_details.all_stations`에 보존합니다. 월세·관리비·시키킨·레이킨은 `property`에만 저장하며 가변 비용과 중복하지 않습니다. 회사 일반 안내는 현재 매물에 적용된다는 근거가 없으면 `reference_information`에만 보존합니다.

오류 응답은 프론트엔드에서 구분할 수 있도록 공통 형식으로 반환합니다.

```json
{
  "error": {
    "code": "GEMINI_RATE_LIMIT",
    "message": "Gemini 요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.",
    "request_id": "01a8ffbce49a",
    "retryable": true
  }
}
```

- `400`: 이미지 형식 또는 입력값 오류
- `403`: URL Context 유료벽 또는 안전 정책 차단
- `413`: 이미지 개수 또는 용량 제한 초과
- `422`: URL 등 요청 형식 오류
- `429`: Gemini 요청 한도 초과
- `500`: 서버 설정 또는 내부 오류
- `502`: Gemini 응답 또는 URL Context 오류
- `504`: Gemini 응답 시간 초과

오류 응답의 `request_id`는 응답 헤더의 `X-Request-ID` 및 서버 로그의 요청 ID와 같습니다. `retryable`이 `true`이면 잠시 후 같은 요청을 다시 시도할 수 있습니다.

Interactions API의 상태가 `incomplete` 또는 `budget_exceeded`이면 `GEMINI_RESPONSE_TRUNCATED`로 처리합니다. URL Context가 제공하는 `error`, `paywall`, `unsafe` 상태도 각각 구분합니다. 일반 로그인 페이지는 Gemini가 별도의 상태를 제공하지 않으므로 확정 판정하지 않습니다.

## 테스트

```powershell
python -m pytest
```
