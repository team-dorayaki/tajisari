import { create } from "zustand"

import { analyzePropertyCosts, fetchPropertyCosts, saveProperty } from "@/features/properties/api/property-costs-api"
import type { CostTiming, PropertyCostItem, PropertyCostReviewResponse, RequiredCostConfirmation } from "@/features/properties/types/property-costs"

type VerificationType = "AMOUNT" | "OCCURRENCE_TIMING" | "REQUIREDNESS" | "BROKER_CONFIRMATION"
type VerificationStatus = "PENDING" | "RESOLVED"
type CalculationPeriod = "INITIAL" | "MONTHLY" | "FUTURE"

type CostVerification = {
  id: string
  type: VerificationType
  status: VerificationStatus
  answer: string | null
}

type CostItem = {
  id: string
  label: string
  amount: number | null
  currency: "JPY"
  calculationPeriod: CalculationPeriod
  originalText: string | null
  description?: string
  emptyLabel?: string
  monthly?: boolean
  selectable?: boolean
  selected?: boolean
  conditional?: boolean
  verifications: CostVerification[]
}

type CostSectionData = {
  id: string
  title: string
  showItemCount?: boolean
  items: CostItem[]
}

type PropertyInfo = {
  name: string
  area: string
  moveInDate: string
  contractMonths: string
}

type SubmissionStatus = "idle" | "saving" | "analyzing" | "saveError" | "analysisError" | "success"

type SubmissionError = {
  id: number
  stage: "save" | "analysis"
  message: string
}

type ReviewStatus = "idle" | "loading" | "success" | "error"

type PropertyCostsState = {
  propertyInfo: PropertyInfo
  siteInitialCost: number
  costSections: CostSectionData[]
  requiredConfirmations: RequiredCostConfirmation[]
  draftVerificationAnswers: Record<string, string>
  draftSelectedCostIds: string[] | null
  submissionStatus: SubmissionStatus
  reviewStatus: ReviewStatus
  reviewError: string | null
  savedPropertyId: string | null
  submissionError: SubmissionError | null
  updatePropertyInfo: (propertyInfo: PropertyInfo) => void
  updateCostAmounts: (values: Record<string, string>) => void
  updateDraftSelectedCostIds: (ids: string[]) => void
  updateDraftVerification: (verificationId: string, answer: string) => void
  saveReviewDraft: () => void
  discardReviewDraft: () => void
  clearSubmissionError: () => void
  loadPropertyCosts: () => Promise<void>
  submitPropertyAndAnalyze: () => Promise<string>
}

function toCalculationPeriod(timing: CostTiming): CalculationPeriod {
  if (timing === "MONTHLY") return "MONTHLY"
  if (timing === "INITIAL") return "INITIAL"
  return "FUTURE"
}

function toDisplayCostItem(item: PropertyCostItem, confirmations: RequiredCostConfirmation[]): CostItem {
  return {
    id: item.costItemId,
    label: item.displayName,
    amount: item.amount,
    currency: "JPY",
    calculationPeriod: toCalculationPeriod(item.timing),
    originalText: item.rawValue,
    description: item.timing === "CONDITIONAL" ? "1년 미만 퇴거 시" : undefined,
    emptyLabel: item.amount === null ? "미표기" : undefined,
    monthly: item.timing === "MONTHLY",
    selectable: item.obligationStatus === "OPTIONAL",
    selected: item.obligationStatus === "OPTIONAL" ? item.costItemId !== "internet" : undefined,
    conditional: item.timing === "CONDITIONAL",
    verifications: confirmations
      .filter((confirmation) => confirmation.costItemId === item.costItemId)
      .map((confirmation) => ({
        id: confirmation.confirmationId,
        type: confirmation.type,
        status: confirmation.status,
        answer: confirmation.answer,
      })),
  }
}

function toCostSections(response: PropertyCostReviewResponse, confirmations: RequiredCostConfirmation[]): CostSectionData[] {
  const { property, costItems } = response
  const fixedItems: CostItem[] = [
    { id: "rent", label: "월세(家賃)", amount: property.rent, currency: "JPY", calculationPeriod: "INITIAL", originalText: null, verifications: [] },
    { id: "management", label: "관리비·공익비", amount: property.managementFee, currency: "JPY", calculationPeriod: "INITIAL", originalText: null, verifications: [] },
    { id: "deposit", label: "시키킨(敷金)", amount: property.deposit, currency: "JPY", calculationPeriod: "INITIAL", originalText: null, verifications: [] },
    { id: "key-money", label: "레이킨(礼金)", amount: property.keyMoney === 0 ? null : property.keyMoney, currency: "JPY", calculationPeriod: "INITIAL", originalText: null, emptyLabel: property.keyMoney === 0 ? "없음" : undefined, verifications: [] },
  ]
  const displayItems = costItems.map((item) => toDisplayCostItem(item, confirmations))

  return [
    { id: "base", title: "기본 비용", items: fixedItems },
    { id: "move-in", title: "입주 시 추가 비용", showItemCount: true, items: displayItems.filter((item) => item.calculationPeriod === "INITIAL") },
    { id: "monthly", title: "매월 추가 비용", showItemCount: true, items: displayItems.filter((item) => item.calculationPeriod === "MONTHLY") },
    { id: "renewal-exit", title: "갱신·퇴거 비용", showItemCount: true, items: displayItems.filter((item) => item.calculationPeriod === "FUTURE") },
  ]
}

const timingPeriod: Record<string, CalculationPeriod> = {
  MOVE_IN: "INITIAL",
  MONTHLY: "MONTHLY",
  RENEWAL: "FUTURE",
  MOVE_OUT: "FUTURE",
}

const usePropertyCostsStore = create<PropertyCostsState>((set, get) => ({
  propertyInfo: { name: "", area: "", moveInDate: "", contractMonths: "" },
  siteInitialCost: 0,
  costSections: [],
  requiredConfirmations: [],
  draftVerificationAnswers: {},
  draftSelectedCostIds: null,
  submissionStatus: "idle",
  reviewStatus: "idle",
  reviewError: null,
  savedPropertyId: null,
  submissionError: null,
  updatePropertyInfo: (propertyInfo) =>
    set({
      propertyInfo,
      submissionStatus: "idle",
      savedPropertyId: null,
      submissionError: null,
    }),
  updateCostAmounts: (values) =>
    set((state) => ({
      costSections: state.costSections.map((section) => ({
        ...section,
        items: section.items.map((item) =>
          item.id in values ? { ...item, amount: values[item.id] ? Number(values[item.id]) : null } : item,
        ),
      })),
      submissionStatus: "idle",
      savedPropertyId: null,
      submissionError: null,
    })),
  updateDraftSelectedCostIds: (draftSelectedCostIds) => set({ draftSelectedCostIds }),
  updateDraftVerification: (verificationId, answer) =>
    set((state) => ({ draftVerificationAnswers: { ...state.draftVerificationAnswers, [verificationId]: answer } })),
  saveReviewDraft: () =>
    set((state) => ({
      costSections: state.costSections.map((section) => ({
        ...section,
        items: section.items.map((item) => {
          const resolvedVerifications = item.verifications.map((verification) => {
            const answer = state.draftVerificationAnswers[verification.id]
            return answer === undefined ? verification : { ...verification, status: "RESOLVED" as const, answer }
          })
          const amountAnswer = resolvedVerifications.find((verification) => verification.type === "AMOUNT" && verification.status === "RESOLVED")?.answer
          const timingAnswer = resolvedVerifications.find((verification) => verification.type === "OCCURRENCE_TIMING" && verification.status === "RESOLVED")?.answer

          return {
            ...item,
            amount: amountAnswer ? Number(amountAnswer) : item.amount,
            calculationPeriod: timingAnswer ? timingPeriod[timingAnswer] ?? item.calculationPeriod : item.calculationPeriod,
            selected: item.selectable && state.draftSelectedCostIds ? state.draftSelectedCostIds.includes(item.id) : item.selected,
            verifications: resolvedVerifications,
          }
        }),
      })),
      requiredConfirmations: state.requiredConfirmations.map((confirmation) => {
        const answer = state.draftVerificationAnswers[confirmation.confirmationId]
        return answer === undefined ? confirmation : { ...confirmation, status: "RESOLVED" as const, answer }
      }),
      draftVerificationAnswers: {},
      draftSelectedCostIds: null,
      submissionStatus: "idle",
      savedPropertyId: null,
      submissionError: null,
    })),
  discardReviewDraft: () => set({ draftVerificationAnswers: {}, draftSelectedCostIds: null }),
  clearSubmissionError: () => set({ submissionError: null }),
  loadPropertyCosts: async () => {
    if (get().reviewStatus === "loading") return
    set({ reviewStatus: "loading", reviewError: null })

    try {
      const response = await fetchPropertyCosts()
      const { requiredConfirmations } = response
      set({
        propertyInfo: {
          name: response.property.name,
          area: response.property.area,
          moveInDate: response.property.availableFrom ?? "",
          contractMonths: response.property.contractPeriodMonths?.toString() ?? "",
        },
        siteInitialCost: response.property.listedInitialCostTotal ?? 0,
        costSections: toCostSections(response, requiredConfirmations),
        requiredConfirmations,
        savedPropertyId: response.property.propertyId,
        reviewStatus: "success",
        reviewError: null,
      })
    } catch {
      set({ reviewStatus: "error", reviewError: "비용 정보를 불러오지 못했어요. 다시 시도해주세요." })
    }
  },
  submitPropertyAndAnalyze: async () => {
    let propertyId = get().savedPropertyId

    if (!propertyId) {
      set({ submissionStatus: "saving", submissionError: null })
      const { propertyInfo, siteInitialCost, costSections } = get()

      try {
        const response = await saveProperty({ propertyInfo, siteInitialCost, costSections })
        propertyId = response.propertyId
        set({ savedPropertyId: propertyId })
      } catch (error) {
        set((state) => ({
          submissionStatus: "saveError",
          submissionError: {
            id: (state.submissionError?.id ?? 0) + 1,
            stage: "save",
            message: "매물 저장에 실패했어요. 다시 시도해주세요.",
          },
        }))
        throw error
      }
    }

    set({ submissionStatus: "analyzing", submissionError: null })

    try {
      await analyzePropertyCosts(propertyId)
      set({ submissionStatus: "success" })
      return propertyId
    } catch (error) {
      set((state) => ({
        submissionStatus: "analysisError",
        submissionError: {
          id: (state.submissionError?.id ?? 0) + 1,
          stage: "analysis",
          message: "비용 분석에 실패했어요. 다시 시도해주세요.",
        },
      }))
      throw error
    }
  },
}))

export { usePropertyCostsStore }
export type { CalculationPeriod, CostItem, CostSectionData, CostVerification, PropertyInfo, SubmissionStatus, VerificationType }
