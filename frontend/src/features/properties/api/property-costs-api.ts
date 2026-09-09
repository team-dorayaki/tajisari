import type { PropertyCostReviewResponse } from "@/features/properties/types/property-costs"

type SavePropertyRequest = {
  sourceType: "IMAGE" | "URL"
  modelVersion: string
  property: {
    sourceSite: string
    sourceUrl: string | null
    propertyName: string
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
  propertyCostItems: Array<{ rawName: string; displayName: string; amount: number | null; rawValue: string | null; obligationStatus: "REQUIRED" | "OPTIONAL" | "UNKNOWN"; includedInCalculation: boolean; timing: "INITIAL" | "MONTHLY" | "RENEWAL" | "MOVE_OUT" | "CONDITIONAL" | "UNKNOWN" }>
  rawResult: unknown
}

type SavePropertyResponse = {
  propertyId: number
}

type MockFailureStage = "review" | "save" | "analysis"

const mockPropertyCosts: PropertyCostReviewResponse = {
  property: {
    propertyId: "shinjuku-room-a",
    sourceSite: "SUUMO",
    sourceUrl: "https://suumo.jp/chintai/mock/shinjuku-room-a",
    name: "신주쿠 원룸 A",
    area: "도쿄도 신주쿠",
    rent: 78_000,
    managementFee: 6_000,
    deposit: 78_000,
    keyMoney: 0,
    availableFrom: "2026-10-15",
    contractPeriodMonths: 24,
    listedInitialCostTotal: 320_000,
    createdAt: "2026-09-08T10:00:00Z",
    updatedAt: "2026-09-08T10:00:00Z",
  },
  costItems: [
    { costItemId: "prepaid-rent", propertyId: "shinjuku-room-a", rawName: "前家賃", displayName: "선불 월세", amount: 84_000, rawValue: "前家賃 84,000円", obligationStatus: "REQUIRED", timing: "INITIAL", createdAt: "2026-09-08T10:00:00Z", updatedAt: "2026-09-08T10:00:00Z" },
    { costItemId: "brokerage", propertyId: "shinjuku-room-a", rawName: "仲介手数料", displayName: "중개수수료", amount: 85_800, rawValue: "仲介手数料 85,800円", obligationStatus: "REQUIRED", timing: "INITIAL", createdAt: "2026-09-08T10:00:00Z", updatedAt: "2026-09-08T10:00:00Z" },
    { costItemId: "guarantor", propertyId: "shinjuku-room-a", rawName: "初回保証料", displayName: "초기 보증회사료", amount: 42_000, rawValue: "初回保証料 42,000円", obligationStatus: "REQUIRED", timing: "INITIAL", createdAt: "2026-09-08T10:00:00Z", updatedAt: "2026-09-08T10:00:00Z" },
    { costItemId: "key-replacement", propertyId: "shinjuku-room-a", rawName: "鍵交換費", displayName: "열쇠교체비", amount: 22_000, rawValue: "鍵交換費 22,000円", obligationStatus: "UNKNOWN", timing: "INITIAL", createdAt: "2026-09-08T10:00:00Z", updatedAt: "2026-09-08T10:00:00Z" },
    { costItemId: "fire-insurance", propertyId: "shinjuku-room-a", rawName: "損害保険料", displayName: "화재보험료", amount: null, rawValue: "損保 要・金額記載なし", obligationStatus: "REQUIRED", timing: "INITIAL", createdAt: "2026-09-08T10:00:00Z", updatedAt: "2026-09-08T10:00:00Z" },
    { costItemId: "antibacterial", propertyId: "shinjuku-room-a", rawName: "抗菌消毒費", displayName: "항균·소독비", amount: 18_000, rawValue: "抗菌消毒費 18,000円", obligationStatus: "OPTIONAL", timing: "INITIAL", createdAt: "2026-09-08T10:00:00Z", updatedAt: "2026-09-08T10:00:00Z" },
    { costItemId: "paperwork", propertyId: "shinjuku-room-a", rawName: "書類作成費", displayName: "서류작성비", amount: 11_000, rawValue: "書類作成費 11,000円", obligationStatus: "UNKNOWN", timing: "INITIAL", createdAt: "2026-09-08T10:00:00Z", updatedAt: "2026-09-08T10:00:00Z" },
    { costItemId: "monthly-guarantee", propertyId: "shinjuku-room-a", rawName: "月額保証料", displayName: "월 보증료", amount: 1_200, rawValue: "月額保証料 1,200円", obligationStatus: "REQUIRED", timing: "MONTHLY", createdAt: "2026-09-08T10:00:00Z", updatedAt: "2026-09-08T10:00:00Z" },
    { costItemId: "water", propertyId: "shinjuku-room-a", rawName: "水道使用料", displayName: "수도사용료", amount: 3_000, rawValue: "水道使用料 3,000円", obligationStatus: "REQUIRED", timing: "MONTHLY", createdAt: "2026-09-08T10:00:00Z", updatedAt: "2026-09-08T10:00:00Z" },
    { costItemId: "support", propertyId: "shinjuku-room-a", rawName: "24時間サポート", displayName: "24시간 서포트", amount: 880, rawValue: "24時間サポート 880円/月", obligationStatus: "OPTIONAL", timing: "MONTHLY", createdAt: "2026-09-08T10:00:00Z", updatedAt: "2026-09-08T10:00:00Z" },
    { costItemId: "internet", propertyId: "shinjuku-room-a", rawName: "インターネット利用料", displayName: "인터넷 이용료", amount: null, rawValue: null, obligationStatus: "OPTIONAL", timing: "MONTHLY", createdAt: "2026-09-08T10:00:00Z", updatedAt: "2026-09-08T10:00:00Z" },
    { costItemId: "renewal-guarantee", propertyId: "shinjuku-room-a", rawName: "保証更新料", displayName: "보증 갱신료", amount: 10_000, rawValue: "保証更新料 10,000円", obligationStatus: "REQUIRED", timing: "RENEWAL", createdAt: "2026-09-08T10:00:00Z", updatedAt: "2026-09-08T10:00:00Z" },
    { costItemId: "contract-renewal", propertyId: "shinjuku-room-a", rawName: "更新料", displayName: "계약 갱신료", amount: 78_000, rawValue: "更新料 1ヶ月", obligationStatus: "REQUIRED", timing: "RENEWAL", createdAt: "2026-09-08T10:00:00Z", updatedAt: "2026-09-08T10:00:00Z" },
    { costItemId: "cleaning", propertyId: "shinjuku-room-a", rawName: "クリーニング費", displayName: "퇴거 청소비", amount: 33_000, rawValue: "クリーニング費 33,000円", obligationStatus: "UNKNOWN", timing: "UNKNOWN", createdAt: "2026-09-08T10:00:00Z", updatedAt: "2026-09-08T10:00:00Z" },
    { costItemId: "early-cancellation", propertyId: "shinjuku-room-a", rawName: "短期解約違約金", displayName: "단기해약 위약금", amount: 78_000, rawValue: "1年未満の退去時 賃料1ヶ月分", obligationStatus: "REQUIRED", timing: "CONDITIONAL", createdAt: "2026-09-08T10:00:00Z", updatedAt: "2026-09-08T10:00:00Z" },
  ],
  requiredConfirmations: [
    { confirmationId: "verify-key-requiredness", costItemId: "key-replacement", type: "REQUIREDNESS", status: "PENDING", answer: null },
    { confirmationId: "verify-fire-amount", costItemId: "fire-insurance", type: "AMOUNT", status: "PENDING", answer: null },
    { confirmationId: "verify-paperwork-broker", costItemId: "paperwork", type: "BROKER_CONFIRMATION", status: "PENDING", answer: null },
    { confirmationId: "verify-cleaning-timing", costItemId: "cleaning", type: "OCCURRENCE_TIMING", status: "PENDING", answer: null },
  ],
}

function consumeMockFailure(stage: MockFailureStage) {
  if (!import.meta.env.DEV || typeof window === "undefined") return

  const url = new URL(window.location.href)
  if (url.searchParams.get("mockApiFailure") !== stage) return

  url.searchParams.delete("mockApiFailure")
  window.history.replaceState(window.history.state, "", url)
  throw new Error(`Mock ${stage} failure`)
}

function waitForMockResponse(delay = 500) {
  return new Promise((resolve) => globalThis.setTimeout(resolve, delay))
}

async function fetchPropertyCosts(): Promise<PropertyCostReviewResponse> {
  await waitForMockResponse()
  consumeMockFailure("review")
  return structuredClone(mockPropertyCosts)
}

async function parseSavePropertyResponse(response: Response): Promise<SavePropertyResponse> {
  const body = await response.json().catch(() => null) as { success: boolean; data: { propertyId?: number } | null; error?: { message?: string } | null } | null
  const propertyId = body?.data?.propertyId
  if (!response.ok || !body?.success || body.data === null || !Number.isSafeInteger(propertyId) || propertyId === undefined || propertyId <= 0) {
    throw new Error(body?.error?.message ?? "매물 저장에 실패했어요. 다시 시도해주세요.")
  }
  return { propertyId }
}

async function confirmUrlProperty(payload: SavePropertyRequest): Promise<SavePropertyResponse> {
  const response = await fetch("/api/properties/confirm", {
    method: "POST",
    credentials: "same-origin",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  })
  return parseSavePropertyResponse(response)
}

async function confirmImageProperty(payload: SavePropertyRequest, files: File[]): Promise<SavePropertyResponse> {
  const formData = new FormData()
  formData.append("request", new Blob([JSON.stringify(payload)], { type: "application/json" }))
  files.forEach((file) => formData.append("files", file))

  const response = await fetch("/api/properties/confirm/images", {
    method: "POST",
    credentials: "same-origin",
    body: formData,
  })
  return parseSavePropertyResponse(response)
}

async function saveProperty(payload: SavePropertyRequest, files: File[] = []): Promise<SavePropertyResponse> {
  return payload.sourceType === "IMAGE"
    ? confirmImageProperty(payload, files)
    : confirmUrlProperty(payload)
}

async function analyzePropertyCosts(propertyId: string): Promise<void> {
  void propertyId
  await waitForMockResponse(3000)
  consumeMockFailure("analysis")
}

export { analyzePropertyCosts, fetchPropertyCosts, saveProperty }
export type { SavePropertyRequest, SavePropertyResponse }
