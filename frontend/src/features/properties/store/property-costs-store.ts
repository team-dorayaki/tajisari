import { create } from "zustand"

import { calculateProperty, saveProperty } from "@/features/properties/api/property-costs-api"

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

type SubmissionStatus = "idle" | "saving" | "calculating" | "saveError" | "calculationError" | "success"

type SubmissionError = {
  id: number
  stage: "save" | "calculation"
  message: string
}

type PropertyCostsState = {
  propertyInfo: PropertyInfo
  siteInitialCost: number
  costSections: CostSectionData[]
  draftVerificationAnswers: Record<string, string>
  draftSelectedCostIds: string[] | null
  submissionStatus: SubmissionStatus
  savedPropertyId: string | null
  submissionError: SubmissionError | null
  updatePropertyInfo: (propertyInfo: PropertyInfo) => void
  updateCostAmounts: (values: Record<string, string>) => void
  updateDraftSelectedCostIds: (ids: string[]) => void
  updateDraftVerification: (verificationId: string, answer: string) => void
  saveReviewDraft: () => void
  discardReviewDraft: () => void
  clearSubmissionError: () => void
  submitPropertyAndCalculate: () => Promise<string>
}

const mockCostSections: CostSectionData[] = [
  {
    id: "base",
    title: "기본 비용",
    items: [
      { id: "rent", label: "월세(家賃)", amount: 78_000, currency: "JPY", calculationPeriod: "INITIAL", originalText: null, verifications: [] },
      { id: "management", label: "관리비·공익비", amount: 6_000, currency: "JPY", calculationPeriod: "INITIAL", originalText: null, verifications: [] },
      { id: "deposit", label: "시키킨(敷金)", amount: 78_000, currency: "JPY", calculationPeriod: "INITIAL", originalText: null, verifications: [] },
      { id: "key-money", label: "레이킨(礼金)", amount: null, currency: "JPY", calculationPeriod: "INITIAL", originalText: null, emptyLabel: "없음", verifications: [] },
    ],
  },
  {
    id: "move-in",
    title: "입주 시 추가 비용",
    showItemCount: true,
    items: [
      { id: "prepaid-rent", label: "선불 월세(前家賃)", amount: 84_000, currency: "JPY", calculationPeriod: "INITIAL", originalText: null, verifications: [] },
      { id: "brokerage", label: "중개수수료", amount: 85_800, currency: "JPY", calculationPeriod: "INITIAL", originalText: null, verifications: [] },
      { id: "guarantor", label: "초기 보증회사료", amount: 42_000, currency: "JPY", calculationPeriod: "INITIAL", originalText: null, verifications: [] },
      {
        id: "key-replacement",
        label: "열쇠교체비",
        amount: 22_000,
        currency: "JPY",
        calculationPeriod: "INITIAL",
        originalText: "鍵交換費 22,000円",
        verifications: [{ id: "verify-key-requiredness", type: "REQUIREDNESS", status: "PENDING", answer: null }],
      },
      {
        id: "fire-insurance",
        label: "화재보험료",
        amount: null,
        currency: "JPY",
        calculationPeriod: "INITIAL",
        originalText: "損保 要・金額記載なし",
        verifications: [{ id: "verify-fire-amount", type: "AMOUNT", status: "PENDING", answer: null }],
      },
      { id: "antibacterial", label: "항균·소독비", amount: 18_000, currency: "JPY", calculationPeriod: "INITIAL", originalText: null, selectable: true, selected: true, verifications: [] },
      {
        id: "paperwork",
        label: "서류작성비",
        amount: 11_000,
        currency: "JPY",
        calculationPeriod: "INITIAL",
        originalText: "書類作成費 11,000円",
        verifications: [{ id: "verify-paperwork-broker", type: "BROKER_CONFIRMATION", status: "PENDING", answer: null }],
      },
    ],
  },
  {
    id: "monthly",
    title: "매월 추가 비용",
    showItemCount: true,
    items: [
      { id: "monthly-guarantee", label: "월 보증료", amount: 1_200, currency: "JPY", calculationPeriod: "MONTHLY", originalText: null, monthly: true, verifications: [] },
      { id: "water", label: "수도사용료", amount: 3_000, currency: "JPY", calculationPeriod: "MONTHLY", originalText: null, monthly: true, verifications: [] },
      { id: "support", label: "24시간 서포트", amount: 880, currency: "JPY", calculationPeriod: "MONTHLY", originalText: null, monthly: true, selectable: true, selected: true, verifications: [] },
      { id: "internet", label: "인터넷 이용료", amount: null, currency: "JPY", calculationPeriod: "MONTHLY", originalText: null, monthly: true, selectable: true, selected: false, verifications: [] },
    ],
  },
  {
    id: "renewal-exit",
    title: "갱신·퇴거 비용",
    showItemCount: true,
    items: [
      { id: "renewal-guarantee", label: "보증 갱신료", amount: 10_000, currency: "JPY", calculationPeriod: "FUTURE", originalText: null, verifications: [] },
      { id: "contract-renewal", label: "계약 갱신료", amount: 78_000, currency: "JPY", calculationPeriod: "FUTURE", originalText: null, verifications: [] },
      {
        id: "cleaning",
        label: "퇴거 청소비",
        amount: 33_000,
        currency: "JPY",
        calculationPeriod: "FUTURE",
        originalText: "クリーニング費 33,000円",
        verifications: [{ id: "verify-cleaning-timing", type: "OCCURRENCE_TIMING", status: "PENDING", answer: null }],
      },
      { id: "early-cancellation", label: "단기해약 위약금", amount: 78_000, currency: "JPY", calculationPeriod: "FUTURE", originalText: null, description: "1년 미만 퇴거 시", conditional: true, verifications: [] },
    ],
  },
]

const timingPeriod: Record<string, CalculationPeriod> = {
  MOVE_IN: "INITIAL",
  MONTHLY: "MONTHLY",
  RENEWAL: "FUTURE",
  MOVE_OUT: "FUTURE",
}

const usePropertyCostsStore = create<PropertyCostsState>((set, get) => ({
  propertyInfo: { name: "신주쿠 원룸 A", area: "도쿄도 신주쿠", moveInDate: "2026-10-15", contractMonths: "24" },
  siteInitialCost: 320_000,
  costSections: mockCostSections,
  draftVerificationAnswers: {},
  draftSelectedCostIds: null,
  submissionStatus: "idle",
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
      draftVerificationAnswers: {},
      draftSelectedCostIds: null,
      submissionStatus: "idle",
      savedPropertyId: null,
      submissionError: null,
    })),
  discardReviewDraft: () => set({ draftVerificationAnswers: {}, draftSelectedCostIds: null }),
  clearSubmissionError: () => set({ submissionError: null }),
  submitPropertyAndCalculate: async () => {
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

    set({ submissionStatus: "calculating", submissionError: null })

    try {
      await calculateProperty(propertyId)
      set({ submissionStatus: "success" })
      return propertyId
    } catch (error) {
      set((state) => ({
        submissionStatus: "calculationError",
        submissionError: {
          id: (state.submissionError?.id ?? 0) + 1,
          stage: "calculation",
          message: "비용 계산에 실패했어요. 다시 시도해주세요.",
        },
      }))
      throw error
    }
  },
}))

export { usePropertyCostsStore }
export type { CalculationPeriod, CostItem, CostSectionData, CostVerification, PropertyInfo, SubmissionStatus, VerificationType }
