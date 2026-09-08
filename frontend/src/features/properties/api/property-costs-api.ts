import type { CostSectionData, PropertyInfo } from "@/features/properties/store/property-costs-store"

type SavePropertyRequest = {
  propertyInfo: PropertyInfo
  siteInitialCost: number
  costSections: CostSectionData[]
}

type SavePropertyResponse = {
  propertyId: string
}

type MockFailureStage = "save" | "calculation"

function consumeMockFailure(stage: MockFailureStage) {
  if (!import.meta.env.DEV || typeof window === "undefined") return

  const url = new URL(window.location.href)
  if (url.searchParams.get("mockApiFailure") !== stage) return

  url.searchParams.delete("mockApiFailure")
  window.history.replaceState(window.history.state, "", url)
  throw new Error(`Mock ${stage} failure`)
}

function waitForMockResponse() {
  return new Promise((resolve) => globalThis.setTimeout(resolve, 500))
}

async function saveProperty(payload: SavePropertyRequest): Promise<SavePropertyResponse> {
  void payload
  await waitForMockResponse()
  consumeMockFailure("save")

  return { propertyId: globalThis.crypto.randomUUID() }
}

async function calculateProperty(propertyId: string): Promise<void> {
  void propertyId
  await waitForMockResponse()
  consumeMockFailure("calculation")
}

export { calculateProperty, saveProperty }
export type { SavePropertyRequest, SavePropertyResponse }
