type AnalysisCostItem = {
  rawName: string
  displayName: string
  amount: number | null
  rawValue: string | null
  obligationStatus: "REQUIRED" | "OPTIONAL" | "UNKNOWN"
  timing: "INITIAL" | "MONTHLY" | "RENEWAL" | "MOVE_OUT" | "CONDITIONAL" | "UNKNOWN"
}

type PropertyAnalysisResult = {
  inputType: "images" | "url"
  modelVersion: string
  rawResult: unknown
  analysisMetadata: {
    sourceType: "IMAGE" | "URL" | "BOTH"
  }
  analysisDetails: {
    costItemAnalysis: Array<{
      costItemIndex: number
      needsReview: boolean
    }>
  }
  property: {
    sourceSite: string | null
    sourceUrl: string | null
    propertyName: string | null
    prefecture: string | null
    city: string | null
    exclusiveAreaM2: number | null
    nearestStation: string | null
    walkMinutes: number | null
    rent: number | null
    managementFee: number | null
    deposit: number | null
    keyMoney: number | null
    availableFrom: string | null
    contractPeriodMonths: number | null
    listedInitialCostTotal: number | null
  }
  propertyCostItems: AnalysisCostItem[]
}

type ApiResponse<T> = {
  success: boolean
  data: T | null
  error: { code?: string; message?: string } | null
}

async function requestAnalysis(path: string, body: BodyInit, headers?: HeadersInit) {
  const response = await fetch(path, { body, method: "POST", credentials: "same-origin", headers })
  const payload = await response.json().catch(() => null) as ApiResponse<PropertyAnalysisResult> | null

  if (!response.ok || !payload?.success || payload.data === null) {
    throw new Error(payload?.error?.message ?? "매물 분석에 실패했어요. 잠시 후 다시 시도해주세요.")
  }

  return payload.data
}

async function analyzePropertyImages(files: File[]) {
  const formData = new FormData()
  files.forEach((file) => formData.append("files", file))
  return requestAnalysis("/api/property-analyses/images", formData)
}

async function analyzePropertyUrl(url: string) {
  return requestAnalysis("/api/property-analyses/url", JSON.stringify({ url }), { "Content-Type": "application/json" })
}

export { analyzePropertyImages, analyzePropertyUrl }
export type { AnalysisCostItem, PropertyAnalysisResult }
