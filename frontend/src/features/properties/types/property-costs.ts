type SourceSite = "SUUMO" | "HOMES" | "UR" | "OTHER"
type CostObligationStatus = "REQUIRED" | "OPTIONAL" | "UNKNOWN"
type CostTiming = "INITIAL" | "MONTHLY" | "RENEWAL" | "MOVE_OUT" | "CONDITIONAL" | "UNKNOWN"
type CostConfirmationType = "AMOUNT" | "OCCURRENCE_TIMING" | "REQUIREDNESS" | "BROKER_CONFIRMATION"
type CostConfirmationStatus = "PENDING" | "RESOLVED"

type PropertyCostReviewProperty = {
  propertyId: string | null
  sourceSite: SourceSite | null
  sourceUrl: string | null
  name: string
  area: string
  rent: number | null
  managementFee: number | null
  deposit: number | null
  keyMoney: number | null
  availableFrom: string | null
  contractPeriodMonths: number | null
  listedInitialCostTotal: number | null
  createdAt: string
  updatedAt: string
}

type PropertyCostItem = {
  costItemId: string
  propertyId: string | null
  rawName: string
  displayName: string
  amount: number | null
  rawValue: string | null
  obligationStatus: CostObligationStatus
  timing: CostTiming
  createdAt: string
  updatedAt: string
}

type PropertyCostReviewResponse = {
  property: PropertyCostReviewProperty
  costItems: PropertyCostItem[]
  requiredConfirmations: RequiredCostConfirmation[]
}

type RequiredCostConfirmation = {
  confirmationId: string
  costItemId: string
  type: CostConfirmationType
  status: CostConfirmationStatus
  answer: string | null
}

export type {
  CostObligationStatus,
  CostConfirmationStatus,
  CostConfirmationType,
  CostTiming,
  PropertyCostItem,
  PropertyCostReviewProperty,
  PropertyCostReviewResponse,
  RequiredCostConfirmation,
  SourceSite,
}
