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

## 설치

PowerShell에서 `ai` 폴더로 이동한 뒤 실행합니다.

```powershell
..\.venv-new\Scripts\Activate.ps1
python -m pip install -r requirements.txt
$env:GEMINI_API_KEY="본인의_API_KEY"
```

## 서버 실행

```powershell
python -m uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```

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
