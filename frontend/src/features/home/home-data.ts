type HomeStage = "1" | "2" | "3" | "4" | "done"

type HomeStageContent = {
  label: string
  title: string
  description: string
  action?: string
  actionHref?: string
  illustration: string
  progress?: number
}

const homeStageContents: Record<HomeStage, HomeStageContent> = {
  "1": {
    label: "STEP 1",
    title: "정착 계획이 필요해요",
    description: "먼저 내 계획을 등록하기",
    action: "계획 등록하기",
    actionHref: "/plan",
    illustration: "🏠",
    progress: 5,
  },
  "2": {
    label: "STEP 2",
    title: "관심 매물을 분석해보세요",
    description: "이미지로 한 번에 숨은 비용 찾기",
    action: "매물 등록하기",
    actionHref: "/properties/new",
    illustration: "📝",
    progress: 28,
  },
  "3": {
    label: "STEP 3",
    title: "매물을 비교해보세요",
    description: "같은 계획과 환율로 한눈에 비교하기",
    action: "매물 비교하기",
    actionHref: "/properties/compare",
    illustration: "🏘️",
    progress: 52,
  },
  "4": {
    label: "STEP 4",
    title: "최종 매물을 선택해보세요",
    description: "비용과 조건을 확인하고 우선순위 정하기",
    action: "최종 매물 선정하기",
    actionHref: "/properties/compare?tab=conditions",
    illustration: "🏡",
    progress: 77,
  },
  done: {
    label: "COMPLETED",
    title: "이제 출국 준비를 시작해보세요",
    description: "타지살이와 함께 차근차근 준비하기",
    illustration: "✈️",
  },
}

function isHomeStage(value: string | null): value is HomeStage {
  return value === "1" || value === "2" || value === "3" || value === "4" || value === "done"
}

export { homeStageContents, isHomeStage }
export type { HomeStageContent }
