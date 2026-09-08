export type HomeProgress = { stage: "1" | "2" | "3" | "4" | "done" }

// TODO: 사용자 진행상태 API가 준비되면 이 함수의 구현만 실제 요청으로 교체한다.
async function fetchHomeProgress(): Promise<HomeProgress> {
  const planId = window.localStorage.getItem("tajisari.settlementPlanId")
  return { stage: planId ? "2" : "1" }
}

export { fetchHomeProgress }
