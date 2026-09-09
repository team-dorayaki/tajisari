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

function createMockRoomImage(label: string, wall: string, floor: string, accent: string) {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 400"><rect width="800" height="270" fill="${wall}"/><rect y="270" width="800" height="130" fill="${floor}"/><rect x="80" y="66" width="230" height="174" rx="8" fill="#f9fbfc"/><path d="M80 153h230M195 66v174" stroke="#c8d3d8" stroke-width="8"/><rect x="505" y="95" width="168" height="17" rx="8" fill="${accent}"/><rect x="535" y="157" width="138" height="17" rx="8" fill="${accent}"/><rect x="560" y="219" width="113" height="17" rx="8" fill="${accent}"/><ellipse cx="398" cy="346" rx="137" ry="23" fill="#ffffff" fill-opacity=".28"/><text x="36" y="365" fill="#fff" font-family="sans-serif" font-size="22" font-weight="700">${label}</text></svg>`
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`
}

const mockProperties: PropertySummary[] = [
  {
    id: "shinjuku-room-a",
    name: "신주쿠 원룸 A",
    area: "도쿄도 신주쿠",
    moveInDate: "2026-10-15",
    rent: 78_000,
    managementFee: 6_000,
    initialCost: 326_000,
    livingMonths: "4.2개월",
    excludedCosts: [
      { label: "화재보험료", category: "계약·입주 시" },
      { label: "인터넷 이용료", category: "매월 반복비용" },
    ],
    images: [
      { id: "shinjuku-1", src: createMockRoomImage("거실", "#e7ece7", "#d8cfbd", "#34404a"), alt: "신주쿠 원룸 A 거실" },
      { id: "shinjuku-2", src: createMockRoomImage("주방", "#edf1ee", "#d7d0c5", "#657b7c"), alt: "신주쿠 원룸 A 주방" },
      { id: "shinjuku-3", src: createMockRoomImage("수납공간", "#f1f0eb", "#d9d3cb", "#536168"), alt: "신주쿠 원룸 A 수납공간" },
    ],
  },
  {
    id: "yokohama-studio",
    name: "요코하마 스튜디오",
    area: "가나가와현 요코하마",
    moveInDate: "2026-10-01",
    rent: 65_000,
    managementFee: 5_000,
    initialCost: 245_000,
    livingMonths: "5.1개월",
    excludedCosts: [
      { label: "퇴거 청소비", category: "계약 후" },
    ],
    images: [
      { id: "yokohama-1", src: createMockRoomImage("스튜디오", "#e5eef1", "#d4cec2", "#4e7580"), alt: "요코하마 스튜디오 거실" },
      { id: "yokohama-2", src: createMockRoomImage("발코니", "#eaf2ef", "#d0cabc", "#557d70"), alt: "요코하마 스튜디오 발코니" },
      { id: "yokohama-3", src: createMockRoomImage("욕실", "#eff1f4", "#d6d1ca", "#5b718b"), alt: "요코하마 스튜디오 욕실" },
    ],
  },
  {
    id: "osaka-room-b",
    name: "오사카 원룸 B",
    area: "오사카부 오사카",
    moveInDate: "2026-10-20",
    rent: 59_000,
    managementFee: 6_000,
    initialCost: 268_000,
    livingMonths: "4.8개월",
    excludedCosts: [
      { label: "보증회사 이용료", category: "계약·입주 시" },
      { label: "수도요금", category: "매월 반복비용" },
    ],
    images: [
      { id: "osaka-1", src: createMockRoomImage("원룸", "#f0ede8", "#d8cfc2", "#786657"), alt: "오사카 원룸 B 거실" },
      { id: "osaka-2", src: createMockRoomImage("주방", "#eeece7", "#d7cfc4", "#6e7773"), alt: "오사카 원룸 B 주방" },
      { id: "osaka-3", src: createMockRoomImage("창가", "#eceff0", "#d3cbc0", "#546c78"), alt: "오사카 원룸 B 창가" },
    ],
  },
]

type PropertyComparison = {
  propertyId: string
  monthlyHousingCost: number
  initialSettlementCost: number
  balanceAfterMoveIn: number
  livingMonths: number
  refundableDeposit: number
  nonRefundableCost: number
  notes: string[]
}

const mockPropertyComparisons: PropertyComparison[] = [
  {
    propertyId: "shinjuku-room-a",
    monthlyHousingCost: 84_000,
    initialSettlementCost: 348_000,
    balanceAfterMoveIn: 5_040_000,
    livingMonths: 4.2,
    refundableDeposit: 78_000,
    nonRefundableCost: 248_000,
    notes: ["역에서 도보 6분", "관리비 포함 월 비용이 가장 높아요"],
  },
  {
    propertyId: "yokohama-studio",
    monthlyHousingCost: 71_000,
    initialSettlementCost: 245_000,
    balanceAfterMoveIn: 5_790_000,
    livingMonths: 5.1,
    refundableDeposit: 78_000,
    nonRefundableCost: 167_000,
    notes: ["초기 정산 비용이 가장 낮아요", "생활 가능 기간이 가장 길어요"],
  },
  {
    propertyId: "osaka-room-b",
    monthlyHousingCost: 65_000,
    initialSettlementCost: 268_000,
    balanceAfterMoveIn: 5_580_000,
    livingMonths: 4.8,
    refundableDeposit: 78_000,
    nonRefundableCost: 190_000,
    notes: ["월 비용이 가장 낮아요", "이동 거리와 생활권을 확인해보세요"],
  },
]

type MockFailureStage = "list" | "delete" | "compare"

function waitForMockResponse(delay = 300) {
  return new Promise((resolve) => globalThis.setTimeout(resolve, delay))
}

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

async function fetchProperty(propertyId: string): Promise<PropertySummary | null> {
  await waitForMockResponse()
  const property = mockProperties.find((item) => item.id === propertyId)
  return property ? structuredClone(property) : null
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
  await waitForMockResponse()
  consumeMockFailure("compare")
  const comparisonById = new Map(mockPropertyComparisons.map((comparison) => [comparison.propertyId, comparison]))
  return propertyIds.flatMap((propertyId) => {
    const comparison = comparisonById.get(propertyId)
    return comparison ? [structuredClone(comparison)] : []
  })
}

export { deleteProperties, fetchProperties, fetchProperty, fetchPropertyComparisons }
export type { ExcludedCost, PropertyComparison, PropertyImage, PropertySummary }
