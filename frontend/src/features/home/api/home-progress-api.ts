export type HomeProgress = { stage: "1" | "2" | "3" | "4" | "done" }
type HomeProgressApiResponse = { currentStep: "STEP_1" | "STEP_2" | "STEP_3" | "STEP_4" | "COMPLETED" }

const stageByCurrentStep = {
  STEP_1: "1",
  STEP_2: "2",
  STEP_3: "3",
  STEP_4: "4",
  COMPLETED: "done",
} as const

async function fetchHomeProgress(): Promise<HomeProgress> {
  const response = await fetch("/api/me/home-progress")
  const body = await response.json() as { success: boolean; data: HomeProgressApiResponse | null }

  if (!response.ok || !body.success || body.data === null) {
    throw new Error("홈 진행 상태를 불러오지 못했습니다.")
  }

  return { stage: stageByCurrentStep[body.data.currentStep] }
}

export { fetchHomeProgress }
