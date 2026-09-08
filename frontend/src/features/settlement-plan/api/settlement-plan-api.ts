import { JPY_TO_KRW_EXCHANGE_RATE } from "@/constants/currency"

export type SettlementPlan = {
  planId: string
  moveInDate: string
  stayMonths: number
  availableKrw: number
  availableJpy: number
  emergencyKrw: number
  emergencyJpy: number
  additionalKrw: number
  additionalJpy: number
  additionalInputStatus: "direct" | "default" | "not_checked"
  monthlyJpy: number
  monthlyInputMode: "direct" | "default"
  exchangeRate: number
  exchangeRateUpdatedAt: string
}

const mockSettlementPlan: SettlementPlan = {
  planId: "plan-1",
  moveInDate: "2026-10-15",
  stayMonths: 12,
  availableKrw: 8_000_000,
  availableJpy: 100_000,
  emergencyKrw: 1_000_000,
  emergencyJpy: 0,
  additionalKrw: 750_000,
  additionalJpy: 50_000,
  additionalInputStatus: "direct",
  monthlyJpy: 115_000,
  monthlyInputMode: "default",
  exchangeRate: JPY_TO_KRW_EXCHANGE_RATE,
  exchangeRateUpdatedAt: "2026-09-08",
}

const wait = (ms = 350) => new Promise((resolve) => window.setTimeout(resolve, ms))

async function fetchSettlementPlan() {
  await wait()
  return { ...mockSettlementPlan }
}

async function updateSettlementPlan(plan: Partial<SettlementPlan>) {
  await wait()
  Object.assign(mockSettlementPlan, plan)
  return { ...mockSettlementPlan }
}

export { fetchSettlementPlan, updateSettlementPlan }
