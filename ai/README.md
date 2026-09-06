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

```json
{
  "input_type": "images",
  "model": "gemini-3.5-flash-lite",
  "result": {}
}
```

입력 오류는 HTTP 400, Gemini 호출 또는 응답 처리 실패는 HTTP 502로 반환합니다.

## 테스트

```powershell
python -m pytest
```
