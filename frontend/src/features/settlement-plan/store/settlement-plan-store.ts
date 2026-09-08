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

const useSettlementPlanStore = create<SettlementPlanState>((set) => ({
  moveInDate: "2026-10-15",
  stayMonths: 12,
  availableKrw: 8_000_000,
  availableJpy: 100_000,
  emergencyKrw: 1_000_000,
  emergencyJpy: 0,
  additionalCosts: {
    flight: { selected: true, amount: 40_000 },
    moving: { selected: true, amount: 10_000 },
    furniture: { selected: true, amount: 12_000 },
    visa: { selected: false, amount: 0 },
    other: { selected: false, amount: 0 },
  },
  monthlyCosts: defaultMonthlyCosts,
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
}))

export { useSettlementPlanStore }
export type { AdditionalCostKey, MonthlyCostKey }
