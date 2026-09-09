export type HomeProgress = { stage: "1" | "2" | "3" | "4" | "done" }

async function fetchHomeProgress(): Promise<HomeProgress> {
  const response = await fetch("/api/me/home-progress")
  const body = await response.json() as { success: boolean; data: HomeProgress | null }

  if (!response.ok || !body.success || body.data === null) {
    throw new Error("홈 진행 상태를 불러오지 못했습니다.")
  }

  return body.data
}

export { fetchHomeProgress }
