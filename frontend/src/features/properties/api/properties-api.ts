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

function createMockRoomImage(label: string, wall: string, floor: string, accent: string) {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 400"><rect width="800" height="270" fill="${wall}"/><rect y="270" width="800" height="130" fill="${floor}"/><rect x="80" y="66" width="230" height="174" rx="8" fill="#f9fbfc"/><path d="M80 153h230M195 66v174" stroke="#c8d3d8" stroke-width="8"/><rect x="505" y="95" width="168" height="17" rx="8" fill="${accent}"/><rect x="535" y="157" width="138" height="17" rx="8" fill="${accent}"/><rect x="560" y="219" width="113" height="17" rx="8" fill="${accent}"/><ellipse cx="398" cy="346" rx="137" ry="23" fill="#ffffff" fill-opacity=".28"/><text x="36" y="365" fill="#fff" font-family="sans-serif" font-size="22" font-weight="700">${label}</text></svg>`
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`
}

let mockProperties: PropertySummary[] = [
  {
    id: "shinjuku-room-a",
    name: "신주쿠 원룸 A",
    area: "도쿄도 신주쿠",
    moveInDate: "2026-10-15",
    rent: 78_000,
    managementFee: 6_000,
    initialCost: 413_800,
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
]

type MockFailureStage = "list" | "delete"

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
  await waitForMockResponse()
  consumeMockFailure("list")
  return structuredClone(mockProperties)
}

async function fetchProperty(propertyId: string): Promise<PropertySummary | null> {
  await waitForMockResponse()
  const property = mockProperties.find((item) => item.id === propertyId)
  return property ? structuredClone(property) : null
}

async function deleteProperties(propertyIds: string[]): Promise<void> {
  await waitForMockResponse(500)
  consumeMockFailure("delete")
  const ids = new Set(propertyIds)
  mockProperties = mockProperties.filter((property) => !ids.has(property.id))
}

export { deleteProperties, fetchProperties, fetchProperty }
export type { ExcludedCost, PropertyImage, PropertySummary }
