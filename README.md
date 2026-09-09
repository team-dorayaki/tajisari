# 타지살이

일본에서 새로운 생활을 시작하는 청년들을 위한 **정착 준비 지원 서비스**입니다.

워킹홀리데이, 해외취업, 유학 등으로 일본에 처음 정착할 때 발생하는
복잡한 초기 비용과 월 생활비를 정리하고, 매물 정보를 분석해 실제 필요한 자금을 확인할 수 있도록 돕습니다.

## 주요 기능

* 정착 기간과 준비 자금을 기반으로 한 정착 계획 관리
* 매물 이미지·URL 기반 비용 정보 분석
* 초기비용·월 주거비 확인 및 매물 저장·비교

## Tech Stack

**Frontend** React · TypeScript · Vite · Tailwind CSS
**Backend** Java 21 · Spring Boot · JPA · MySQL
**AI** FastAPI · Gemini API

---

## 로컬 실행 방법

### 사전 준비

다음 환경이 필요합니다.

* Java 21
* Node.js
* Python 3
* Docker
* Gemini API Key

---

### 1. Database 실행

프로젝트 루트에서 MySQL을 실행합니다.

```bash
docker compose up -d
```

---

### 2. AI 서버 실행

최초 실행 시 가상환경을 생성하고 의존성을 설치합니다.

```bash
cd ai

python3 -m venv .venv
source .venv/bin/activate

pip install -r requirements.txt
```

Gemini API Key를 설정한 뒤 AI 서버를 실행합니다.

```bash
export GEMINI_API_KEY='YOUR_GEMINI_API_KEY'

python -m uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```

---

### 3. Backend 실행

```bash
cd backend

PROPERTY_IMAGE_STORAGE_ROOT="$PWD/uploads" ./gradlew bootRun
```

Backend는 기본적으로 다음 서버에 연결됩니다.

* MySQL: `localhost:3306`
* AI Server: `localhost:8000`

JPA `ddl-auto: update`를 사용하므로 별도의 테이블 생성 작업은 필요하지 않습니다.

---

### 4. Frontend 실행

최초 실행 시 의존성을 설치합니다.

```bash
cd frontend
npm install
```

개발 서버를 실행합니다.

```bash
npm run dev
```

실행 후 아래 주소로 접속합니다.

```text
http://localhost:5173
```

---

### 실행 순서

```text
Database → AI → Backend → Frontend
```

### 종료

각 서버는 `Ctrl + C`로 종료할 수 있습니다.

Database까지 종료하려면 프로젝트 루트에서 실행합니다.

```bash
docker compose down
```
