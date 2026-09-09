type PropertyImage = {
  id: string
  src: string
  alt: string
}

type ExcludedCost = {
  label: string
  category: string
}

type PropertySummary = {
  id: string
  name: string
  area: string
  moveInDate: string
  rent: number
  managementFee: number
  initialCost: number
  livingMonths: string
  images: PropertyImage[]
  excludedCosts: ExcludedCost[]
}

type ApiResponse<T> = {
  /** 공통 응답 성공 여부 (`success`). */
  success: boolean
  /** 성공 시 목록 페이로드, 실패 시 `null` (`data`). */
  data: T | null
  /** 실패 사유. 목록 화면은 메시지를 사용자에게 노출하지 않고 오류 상태만 표시한다 (`error`). */
  error?: { message?: string } | null
}

type PropertyListItem = {
  /** `properties[].propertyId`: 저장 매물 식별자. 상세 화면 경로에 사용한다. */
  propertyId: number
  /** `properties[].propertyName`: 매물명. 값이 없으면 화면에서 "이름 없는 매물"으로 대체한다. */
  propertyName: string | null
  /** `properties[].rent`: 월 임대료(JPY). */
  rent: number | null
  /** `properties[].initialCost`: 확인된 초기비용 우선의 매물 초기비용(JPY). */
  initialCost: number | null
  /** `properties[].livingMonths`: 최근 정착 계획 기준 생활 가능 기간. 정착 계획이 없으면 `null`. */
  livingMonths: number | null
  /** `properties[].priorityRank`: 1·2순위 또는 미선택 `null`. 목록 우선순위 표시에 아직 연결되지 않았다. */
  priorityRank: 1 | 2 | null
  /** `properties[].thumbnailUrl`: 첫 번째 매물 이미지 URL. 이미지가 없으면 `null`. */
  thumbnailUrl: string | null
}

type PropertyListPayload = {
  /** `data.totalCount`: 저장된 전체 매물 수. 페이지네이션이 없어 현재는 배열 길이로 표시한다. */
  totalCount: number
  /** `data.properties`: 최신 등록순 저장 매물 목록. 비어 있으면 빈 배열이다. */
  properties: PropertyListItem[]
}

type PropertyDetailCostItem = {
  id: string
  label: string
  amount: number | null
  originalText: string | null
  optional: boolean
  selected: boolean
  includedInTotal: boolean
  conditional: boolean
  timing: "INITIAL" | "MONTHLY" | "RENEWAL" | "MOVE_OUT" | "CONDITIONAL" | "UNKNOWN"
}

type PropertyDetailPayload = {
  propertyId: number
  settlementPlanId: number | null
  property: {
    name: string | null
    sourceSite: string | null
    sourceUrl: string | null
    prefecture: string | null
    city: string | null
    area: number | null
    nearestStation: string | null
    walkMinutes: number | null
    availableFrom: string | null
    contractPeriodMonths: number | null
    priorityRank: 1 | 2 | null
    rent: number | null
    managementFee: number | null
  }
  images: Array<{ imageId: number; imageUrl: string; order: number }>
  costAnalysis: {
    summary: {
      initialCost: number | null
      minimumInitialCost: number
      monthlyCost: number | null
      contractMoveInCost: number
      selectedOptionalCost: number
      refundableAmount: number | null
      nonRefundableAmount: number | null
      estimatedMoveOutCost: number
    }
    costGroups: {
      monthly: PropertyDetailCostItem[]
      moveIn: PropertyDetailCostItem[]
      optional: PropertyDetailCostItem[]
      future: PropertyDetailCostItem[]
    }
    excludedCosts: Array<{ label: string; category: string; reason: string }>
  }
  simulation: {
    exchangeRate: { jpy: number; krw: number }
    availableFunds: number
    initialCost: number
    canMoveIn: boolean
    balanceAfterMoveIn: number
    monthlyHousingCost: number
    monthlyLivingCost: number
    totalMonthlyCost: number
    monthlyBalances: Array<{ month: number; balance: number }>
    livingMonths: number | null
    isUnlimited: boolean
    plannedStayMonths: number
    requiredFunds: number
    surplus: number
    shortageJpy: number
    shortageKrw: number
  } | null
}

type PropertyDetail = Omit<PropertyDetailPayload, "images"> & {
  id: string
  name: string
  areaLabel: string
  images: PropertyImage[]
}

type PropertyComparison = {
  propertyId: string
  name: string
  thumbnailUrl: string | null
  monthlyHousingCost: number
  initialSettlementCost: number
  balanceAfterMoveIn: number
  livingMonths: number
  unlimited: boolean
  refundableDeposit: number
  nonRefundableCost: number
  notes: string[]
  lowestInitialSettlementCost: boolean
  lowestMonthlyHousingCost: boolean
  longestLivingMonths: boolean
}

type PropertyComparisonPayload = {
  properties: Array<{
    propertyId: number
    propertyName: string | null
    thumbnailUrl: string | null
    conditions: { prefecture: string | null; city: string | null; nearestStation: string | null; walkMinutes: number | null }
    costs: { initialSettlementCost: number | null; refundableAmount: number | null; nonRefundableAmount: number | null }
    simulation: { balanceAfterMoveIn: number | null; monthlyHousingCost: number | null; livingMonths: number | null; unlimited: boolean } | null
    highlights: { lowestInitialSettlementCost: boolean; lowestMonthlyHousingCost: boolean; longestLivingMonths: boolean }
  }>
}

type MockFailureStage = "list" | "delete"

function consumeMockFailure(stage: MockFailureStage) {
  if (!import.meta.env.DEV || typeof window === "undefined") return

  const url = new URL(window.location.href)
  if (url.searchParams.get("mockApiFailure") !== stage) return

  url.searchParams.delete("mockApiFailure")
  window.history.replaceState(window.history.state, "", url)
  throw new Error(`Mock ${stage} failure`)
}

async function fetchProperties(): Promise<PropertySummary[]> {
  consumeMockFailure("list")
  const response = await fetch("/api/properties", { credentials: "same-origin" })
  const body = await response.json().catch(() => null) as ApiResponse<PropertyListPayload> | null

  if (!response.ok || !body?.success || body.data === null) {
    throw new Error(body?.error?.message ?? `매물 목록을 불러오지 못했습니다. (${response.status})`)
  }

  return body.data.properties.map((property) => ({
    id: String(property.propertyId),
    name: property.propertyName?.trim() || "이름 없는 매물",
    area: "",
    moveInDate: "",
    rent: property.rent ?? 0,
    managementFee: 0,
    initialCost: property.initialCost ?? 0,
    livingMonths: property.livingMonths === null ? "계산 전" : `${property.livingMonths.toFixed(1)}개월`,
    images: property.thumbnailUrl
      ? [{ id: `${property.propertyId}-thumbnail`, src: property.thumbnailUrl, alt: `${property.propertyName ?? "매물"} 썸네일` }]
      : [],
    excludedCosts: [],
  }))
}

async function fetchProperty(propertyId: string): Promise<PropertyDetail> {
  const response = await fetch(`/api/properties/${propertyId}`, { credentials: "same-origin" })
  const body = await response.json().catch(() => null) as ApiResponse<PropertyDetailPayload> | null
  const data = body?.data

  if (!response.ok || !body?.success || data === null || data === undefined) {
    throw new Error(body?.error?.message ?? `매물 상세를 불러오지 못했습니다. (${response.status})`)
  }

  const { property, images } = data
  return {
    ...data,
    id: String(data.propertyId),
    name: property.name?.trim() || "이름 없는 매물",
    areaLabel: [property.prefecture, property.city].filter(Boolean).join(" "),
    images: [...images].sort((left, right) => left.order - right.order).map((image, index) => ({
      id: String(image.imageId),
      src: image.imageUrl,
      alt: `${property.name ?? "매물"} 사진 ${index + 1}`,
    })),
  }
}

async function deleteProperties(propertyIds: string[]): Promise<void> {
  consumeMockFailure("delete")

  for (const propertyId of propertyIds) {
    const response = await fetch(`/api/properties/${propertyId}`, {
      method: "DELETE",
      credentials: "same-origin",
    })
    const text = await response.text()
    const body = text ? JSON.parse(text) as ApiResponse<null> : null

    if (!response.ok || body?.success === false) {
      throw new Error(body?.error?.message ?? `매물을 삭제하지 못했습니다. (${response.status})`)
    }
  }
}

async function fetchPropertyComparisons(propertyIds: string[]): Promise<PropertyComparison[]> {
  const numericIds = propertyIds.map(Number)
  if (numericIds.some((id) => !Number.isSafeInteger(id))) {
    throw new Error("비교할 매물 정보를 확인할 수 없어요.")
  }
  const response = await fetch("/api/property-comparisons", {
    method: "POST",
    credentials: "same-origin",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ propertyIds: numericIds }),
  })
  const body = await response.json().catch(() => null) as ApiResponse<PropertyComparisonPayload> | null
  if (!response.ok || !body?.success || body.data === null) {
    throw new Error(body?.error?.message ?? `매물 비교에 실패했어요. (${response.status})`)
  }
  return body.data.properties.map((property) => ({
    propertyId: String(property.propertyId),
    name: property.propertyName?.trim() || "이름 없는 매물",
    thumbnailUrl: property.thumbnailUrl,
    monthlyHousingCost: property.simulation?.monthlyHousingCost ?? 0,
    initialSettlementCost: property.costs.initialSettlementCost ?? 0,
    balanceAfterMoveIn: property.simulation?.balanceAfterMoveIn ?? 0,
    livingMonths: property.simulation?.livingMonths ?? 0,
    unlimited: property.simulation?.unlimited ?? false,
    refundableDeposit: property.costs.refundableAmount ?? 0,
    nonRefundableCost: property.costs.nonRefundableAmount ?? 0,
    notes: [
      [property.conditions.prefecture, property.conditions.city].filter(Boolean).join(" "),
      property.conditions.nearestStation && property.conditions.walkMinutes !== null ? `${property.conditions.nearestStation} 도보 ${property.conditions.walkMinutes}분` : property.conditions.nearestStation,
    ].filter((note): note is string => Boolean(note)),
    lowestInitialSettlementCost: property.highlights.lowestInitialSettlementCost,
    lowestMonthlyHousingCost: property.highlights.lowestMonthlyHousingCost,
    longestLivingMonths: property.highlights.longestLivingMonths,
  }))
}

export { deleteProperties, fetchProperties, fetchProperty, fetchPropertyComparisons }
export type { ExcludedCost, PropertyComparison, PropertyDetail, PropertyDetailCostItem, PropertyImage, PropertySummary }
