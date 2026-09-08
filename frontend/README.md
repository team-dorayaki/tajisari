# 타지살이 프론트엔드

일본 정착을 준비하는 사용자가 자신의 자금과 예상 생활비를 입력하고, 관심 매물의 초기비용과 생활 가능 기간을 비교할 수 있도록 돕는 모바일 퍼스트 웹앱입니다.

## 기술 스택

- React 19, TypeScript, Vite
- Tailwind CSS 4, shadcn/ui, Radix UI
- React Router
- Zustand
- React Hook Form, Zod
- Lucide React

## 실행 방법

Node.js 24 버전을 사용합니다.

```bash
npm install
npm run dev
```

개발 서버가 안내하는 로컬 주소에서 화면을 확인할 수 있습니다.

## 명령어

```bash
npm run dev      # 개발 서버 실행
npm run lint     # ESLint 검사
npm run build    # 타입 검사 및 프로덕션 빌드
npm run preview  # 빌드 결과 미리보기
```

## 폴더 구조

```text
src/
├── app/                 # 라우터와 앱 진입점
├── components/
│   ├── ui/              # 범용 UI 컴포넌트
│   └── layout/          # 앱 공통 레이아웃
├── features/            # 기능별 페이지, 컴포넌트, 상태
├── lib/                 # 공통 순수 함수와 유틸리티
├── test/                # 테스트 전역 설정
├── index.css            # 전역 토큰과 기본 스타일
└── main.tsx
```

필요한 폴더는 기능을 구현하는 시점에 추가합니다. 한 화면에서만 사용하는 코드는 해당 기능 폴더에 두고, 여러 기능에서 반복되는 것이 확인된 코드만 공통 영역으로 이동합니다.

## 레이아웃 기준

- 모바일 앱 영역은 `width: 100%`, `max-width: 430px`입니다.
- 430px 이하에서는 기기 너비에 맞게 유동적으로 줄어듭니다.
- 넓은 화면에서는 모바일 앱을 중앙에 배치합니다.
- 데스크톱 전용 레이아웃은 별도 후속 작업으로 확장합니다.

## 상태 관리

- 한 컴포넌트 안에서 끝나는 UI 상태는 React 로컬 상태를 사용합니다.
- 검증이 필요한 복합 폼의 입력값과 유효성 상태는 React Hook Form과 Zod로 관리합니다.
- 화면 간 공유하거나 폼에서 적용한 저장 전 draft는 Zustand로 관리합니다.
- 서버 API가 연결되면 서버 상태는 TanStack Query로 분리합니다.

## 작업 방식

1. 기능을 커밋하지 않은 상태로 구현합니다.
2. lint, build와 브라우저 화면을 검증합니다.
3. 사용자가 직접 화면을 확인하고 피드백을 전달합니다.
4. 피드백 반영과 승인이 끝나면 GitHub 이슈를 생성합니다.
5. `feat/fe-{이슈번호}-{기능명}` 브랜치를 생성합니다.
6. 승인된 변경만 논리적인 단위로 커밋합니다.
7. 요청이 있을 때만 push하고 Draft PR을 생성합니다.

커밋 메시지는 Conventional Commits 형식을 사용합니다.

```text
docs(frontend): 프로젝트 문서 작성
chore(frontend): 개발 기반 설정
feat(frontend): 시작 화면 구현
```
