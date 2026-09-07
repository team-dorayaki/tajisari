import type {
  AdditionalCostKey,
  MonthlyCostKey,
} from "@/features/settlement-plan/store/settlement-plan-store"

const stayMonthOptions = [6, 12, 18, 24]

const additionalCostItems: Array<{
  key: AdditionalCostKey
  label: string
  placeholder: string
}> = [
  { key: "flight", label: "항공권", placeholder: "금액 입력" },
  { key: "moving", label: "이사 수하물", placeholder: "금액 입력" },
  { key: "furniture", label: "가구·가전", placeholder: "금액 입력" },
  { key: "visa", label: "비자 행정절차", placeholder: "선택 후 입력" },
  { key: "other", label: "기타 비용", placeholder: "선택 후 입력" },
]

const monthlyCostItems: Array<{ key: MonthlyCostKey; label: string }> = [
  { key: "food", label: "식비" },
  { key: "transportation", label: "교통비" },
  { key: "utilities", label: "공과금" },
  { key: "communication", label: "통신비" },
  { key: "insurance", label: "보험·세금" },
  { key: "other", label: "기타 생활비" },
]

export { additionalCostItems, monthlyCostItems, stayMonthOptions }
