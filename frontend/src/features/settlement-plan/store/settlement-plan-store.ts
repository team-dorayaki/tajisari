import { create } from "zustand"

type SettlementPlanState = {
  moveInDate: string
  stayMonths: number
  availableKrw: number
  availableJpy: number
  emergencyKrw: number
  emergencyJpy: number
  additionalCosts: Record<AdditionalCostKey, AdditionalCost>
  monthlyCosts: Record<MonthlyCostKey, number>
  monthlyInputMode: "direct" | "default"
  setMoveInDate: (moveInDate: string) => void
  setStayMonths: (stayMonths: number) => void
  setAvailableKrw: (availableKrw: number) => void
  setAvailableJpy: (availableJpy: number) => void
  setEmergencyKrw: (emergencyKrw: number) => void
  setEmergencyJpy: (emergencyJpy: number) => void
  toggleAdditionalCost: (key: AdditionalCostKey) => void
  setAdditionalCostAmount: (key: AdditionalCostKey, amount: number) => void
  setMonthlyCost: (key: MonthlyCostKey, amount: number) => void
  applyMonthlyDefaults: () => void
  setMonthlyInputMode: (monthlyInputMode: "direct" | "default") => void
  hydrate: (data: { moveInDate: string; stayMonths: number; availableKrw: number; availableJpy: number; emergencyKrw: number; emergencyJpy: number; additionalInitialCosts?: Array<{ type: string; amount: number; currency: "KRW" | "JPY" }>; monthlyLivingCosts?: Array<{ type: string; amount: number; currency: "KRW" | "JPY" }>; monthlyInputMode: "direct" | "default" }) => void
}

type AdditionalCostKey = "flight" | "moving" | "furniture" | "visa" | "other"

type AdditionalCost = {
  selected: boolean
  amount: number
}

type MonthlyCostKey =
  | "food"
  | "transportation"
  | "utilities"
  | "communication"
  | "insurance"
  | "other"

const defaultMonthlyCosts: Record<MonthlyCostKey, number> = {
  food: 40_000,
  transportation: 10_000,
  utilities: 12_000,
  communication: 8_000,
  insurance: 25_000,
  other: 20_000,
}

function getDefaultMoveInDate() {
  const date = new Date()
  date.setMonth(date.getMonth() + 1)
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Seoul" }).format(date)
}

const useSettlementPlanStore = create<SettlementPlanState>((set) => ({
  moveInDate: getDefaultMoveInDate(),
  stayMonths: 0,
  availableKrw: 0,
  availableJpy: 0,
  emergencyKrw: 0,
  emergencyJpy: 0,
  additionalCosts: {
    flight: { selected: false, amount: 0 },
    moving: { selected: false, amount: 0 },
    furniture: { selected: false, amount: 0 },
    visa: { selected: false, amount: 0 },
    other: { selected: false, amount: 0 },
  },
  monthlyCosts: { food: 0, transportation: 0, utilities: 0, communication: 0, insurance: 0, other: 0 },
  monthlyInputMode: "direct",
  setMoveInDate: (moveInDate) => set({ moveInDate }),
  setStayMonths: (stayMonths) => set({ stayMonths }),
  setAvailableKrw: (availableKrw) => set({ availableKrw }),
  setAvailableJpy: (availableJpy) => set({ availableJpy }),
  setEmergencyKrw: (emergencyKrw) => set({ emergencyKrw }),
  setEmergencyJpy: (emergencyJpy) => set({ emergencyJpy }),
  toggleAdditionalCost: (key) =>
    set((state) => ({
      additionalCosts: {
        ...state.additionalCosts,
        [key]: {
          ...state.additionalCosts[key],
          selected: !state.additionalCosts[key].selected,
        },
      },
    })),
  setAdditionalCostAmount: (key, amount) =>
    set((state) => ({
      additionalCosts: {
        ...state.additionalCosts,
        [key]: { ...state.additionalCosts[key], amount },
      },
    })),
  setMonthlyCost: (key, amount) =>
    set((state) => ({
      monthlyCosts: { ...state.monthlyCosts, [key]: amount },
      monthlyInputMode: "direct",
    })),
  applyMonthlyDefaults: () =>
    set({ monthlyCosts: { ...defaultMonthlyCosts }, monthlyInputMode: "default" }),
  setMonthlyInputMode: (monthlyInputMode) => set({ monthlyInputMode }),
  hydrate: (data) => set((state) => {
    const additionalCosts = { ...state.additionalCosts }
    const additionalTypeToKey: Record<string, AdditionalCostKey> = { AIRFARE: "flight", MOVING: "moving", FURNITURE_APPLIANCE: "furniture", VISA_ADMINISTRATION: "visa", OTHER: "other" }
    for (const item of data.additionalInitialCosts ?? []) { const key = additionalTypeToKey[item.type]; if (key) additionalCosts[key] = { selected: true, amount: item.amount } }
    const monthlyCosts = { ...state.monthlyCosts }
    const monthlyTypeToKey: Record<string, MonthlyCostKey> = { FOOD: "food", TRANSPORTATION: "transportation", UTILITIES: "utilities", COMMUNICATION: "communication", INSURANCE_TAX: "insurance", OTHER: "other" }
    for (const item of data.monthlyLivingCosts ?? []) { const key = monthlyTypeToKey[item.type]; if (key) monthlyCosts[key] = item.amount }
    return { moveInDate: data.moveInDate, stayMonths: data.stayMonths, availableKrw: data.availableKrw, availableJpy: data.availableJpy, emergencyKrw: data.emergencyKrw, emergencyJpy: data.emergencyJpy, additionalCosts, monthlyCosts, monthlyInputMode: data.monthlyInputMode }
  }),
}))

export { useSettlementPlanStore }
export type { AdditionalCostKey, MonthlyCostKey }
