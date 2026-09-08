import type { AdditionalCostKey, MonthlyCostKey } from "@/features/settlement-plan/store/settlement-plan-store"
import { JPY_TO_KRW_EXCHANGE_RATE } from "@/constants/currency"

type CurrencyAmounts = { krw: number; jpy: number }
type CostItem = { type: string; amount: number; currency: "KRW" | "JPY" }
export type SettlementPlanRequest = {
  moveInDate: string; plannedStayMonths: number; preparedFunds: CurrencyAmounts; emergencyReserve: CurrencyAmounts
  additionalInitialCosts: CostItem[]; monthlyLivingCosts: CostItem[]; monthlyLivingCostInputMethod: "DIRECT" | "DEFAULT"
}
export type SettlementPlan = {
  planId: string; moveInDate: string; stayMonths: number; availableKrw: number; availableJpy: number
  emergencyKrw: number; emergencyJpy: number; additionalKrw: number; additionalJpy: number
  additionalInputStatus: "direct" | "default" | "not_checked"; monthlyJpy: number; monthlyInputMode: "direct" | "default"
  exchangeRate: number; exchangeRateUpdatedAt: string
  additionalInitialCosts?: CostItem[]; monthlyLivingCosts?: CostItem[]
}
type ApiResponse<T> = { success: boolean; data: T | null; error?: { message?: string } | null }
type BackendPlan = SettlementPlanRequest & { planId: number; additionalInitialCostTotals: CurrencyAmounts; monthlyLivingCostTotals: CurrencyAmounts }
const planIdKey = "tajisari.settlementPlanId"
const api = "/api/settlement-plans"
const todayInKorea = () => new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Seoul" }).format(new Date())
const costTypeMap: Record<AdditionalCostKey | MonthlyCostKey, string> = {
  flight: "AIRFARE", moving: "MOVING", furniture: "FURNITURE_APPLIANCE", visa: "VISA_ADMINISTRATION",
  food: "FOOD", transportation: "TRANSPORTATION", utilities: "UTILITIES", communication: "COMMUNICATION",
  insurance: "INSURANCE_TAX", other: "OTHER",
}
function getStoredPlanId() { return typeof window === "undefined" ? null : window.localStorage.getItem(planIdKey) }
function toUiPlan(plan: BackendPlan): SettlementPlan {
  const exchangeRate = Number(window.localStorage.getItem("tajisari.exchangeRate")) || JPY_TO_KRW_EXCHANGE_RATE
  const exchangeRateUpdatedAt = todayInKorea()
  return {
    planId: String(plan.planId), moveInDate: plan.moveInDate, stayMonths: plan.plannedStayMonths,
    availableKrw: plan.preparedFunds.krw, availableJpy: plan.preparedFunds.jpy, emergencyKrw: plan.emergencyReserve.krw, emergencyJpy: plan.emergencyReserve.jpy,
    additionalKrw: plan.additionalInitialCostTotals.krw, additionalJpy: plan.additionalInitialCostTotals.jpy, additionalInputStatus: "direct",
    monthlyJpy: plan.monthlyLivingCostTotals.jpy, monthlyInputMode: plan.monthlyLivingCostInputMethod === "DEFAULT" ? "default" : "direct",
    exchangeRate, exchangeRateUpdatedAt,
    additionalInitialCosts: plan.additionalInitialCosts, monthlyLivingCosts: plan.monthlyLivingCosts,
  }
}
async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, { headers: { "Content-Type": "application/json" }, ...options })
  const text = await response.text()
  let body: ApiResponse<T> | null = null
  if (text) {
    try { body = JSON.parse(text) as ApiResponse<T> } catch { /* handled below */ }
  }
  if (!response.ok || !body?.success || body.data === null) throw new Error(body?.error?.message ?? `정착 계획 API 요청에 실패했습니다. (${response.status})`)
  return body.data
}
async function createSettlementPlan(payload: SettlementPlanRequest) {
  const result = await request<{ planId: number }>(api, { method: "POST", body: JSON.stringify(payload) })
  window.localStorage.setItem(planIdKey, String(result.planId)); return result
}
async function fetchSettlementPlan(planId = getStoredPlanId()) {
  if (!planId) throw new Error("저장된 정착 계획이 없습니다.")
  return toUiPlan(await request<BackendPlan>(`${api}/${planId}`))
}
async function updateSettlementPlan(planId: string, payload: SettlementPlanRequest) {
  return toUiPlan(await request<BackendPlan>(`${api}/${planId}`, { method: "PUT", body: JSON.stringify(payload) }))
}
export { createSettlementPlan, fetchSettlementPlan, updateSettlementPlan, costTypeMap }
