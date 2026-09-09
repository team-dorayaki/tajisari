type AnalysisCostItem = {
  rawName: string
  displayName: string
  amount: number | null
  rawValue: string | null
  obligationStatus: "REQUIRED" | "OPTIONAL" | "UNKNOWN"
  timing: "INITIAL" | "MONTHLY" | "RENEWAL" | "MOVE_OUT" | "CONDITIONAL" | "UNKNOWN"
}

type AnalysisSourceType = "IMAGE" | "URL" | "BOTH"
type AnalysisSourceSite = "SUUMO" | "LIFULL_HOMES" | "ATHOME" | "LEOPALACE21" | "GTN_BEST_ESTATE" | "SOL_HOUSING" | "JAPAN_HOMES" | "UR" | "OTHER" | "UNKNOWN"
type Evidence = {
  sourceType: "IMAGE" | "URL" | "POLICY"
  sourceIndex: number | null
  sourceUrl: string | null
  rawText: string | null
}

type PropertyAnalysisResult = {
  inputType: "images" | "url"
  modelVersion: string
  rawResult: unknown
  analysisMetadata: {
    schemaVersion: string
    sourceType: AnalysisSourceType
    imageCount: number
  }
  analysisDetails: {
    fieldAnalysis: Array<{
      field: string
      rawValue: string | null
      confidence: number | null
      needsReview: boolean
      evidence: Evidence[]
    }>
    costItemAnalysis: Array<{
      costItemIndex: number
      scope: "LISTING_SPECIFIC"
      confidence: number | null
      needsReview: boolean
      evidence: Evidence[]
    }>
    allStations: Array<{
      lineName: string | null
      stationName: string | null
      walkMinutes: number | null
      evidence: Evidence[]
    }>
    additionalFields: Array<{
      category: "PROPERTY" | "BUILDING" | "LOCATION" | "ACCESS" | "CONTRACT" | "CONDITION" | "FACILITY" | "AGENCY" | "LISTING" | "OTHER"
      rawName: string | null
      displayName: string | null
      value: string | null
      unit: string | null
      rawValue: string | null
      confidence: number | null
      needsReview: boolean
      evidence: Evidence[]
    }>
    referenceInformation: Array<{
      category: "COMPANY_POLICY" | "SITE_GUIDE"
      rawText: string | null
      appliesToListing: "YES" | "NO" | "UNKNOWN"
      evidence: Evidence[]
    }>
    validation: {
      conflicts: Array<{
        field: string
        values: string[]
        reason: string | null
        resolution: "RESOLVED" | "UNKNOWN"
        resolvedValue: string | null
        evidence: Evidence[]
      }>
      warnings: string[]
      unknownFields: string[]
      checks: {
        evidenceOnly: boolean
        amountDoesNotImplyRequired: boolean
        zeroAndNullDistinguished: boolean
        duplicatesRemoved: boolean
        conflictsReviewed: boolean
        fixedCostsNotDuplicated: boolean
        listingTermsPreferred: boolean
        amountsMatchRawText: boolean
        requiredStatusHasEvidence: boolean
      }
    }
  }
  property: {
    sourceSite: AnalysisSourceSite | null
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
export type { AnalysisCostItem, AnalysisSourceSite, AnalysisSourceType, PropertyAnalysisResult }
