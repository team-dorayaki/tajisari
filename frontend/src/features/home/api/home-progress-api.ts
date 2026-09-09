import { fetchSettlementPlan } from "@/features/settlement-plan/api/settlement-plan-api"

export type HomeProgress = { stage: "1" | "2" | "3" | "4" | "done" }

// TODO: 사용자 진행상태 API가 준비되면 이 함수의 구현만 실제 요청으로 교체한다.
async function fetchHomeProgress(): Promise<HomeProgress> {
  try {
    await fetchSettlementPlan()
    return { stage: "2" }
  } catch {
    return { stage: "1" }
  }
}

export { fetchHomeProgress }
