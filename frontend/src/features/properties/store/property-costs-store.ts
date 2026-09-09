import { create } from "zustand"

import { saveProperty } from "@/features/properties/api/property-costs-api"
import type { PropertyAnalysisResult } from "@/features/properties/api/property-analysis-api"
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
  sourceIndex?: number
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
  costSections: CostSectionData[]
  requiredConfirmations: RequiredCostConfirmation[]
  draftVerificationAnswers: Record<string, string>
  draftSelectedCostIds: string[] | null
  submissionStatus: SubmissionStatus
  reviewStatus: ReviewStatus
  reviewError: string | null
  savedPropertyId: string | null
  analysisResult: PropertyAnalysisResult | null
  analysisFiles: File[]
  submissionError: SubmissionError | null
  updatePropertyInfo: (propertyInfo: PropertyInfo) => void
  updateCostAmounts: (values: Record<string, string>) => void
  updateDraftSelectedCostIds: (ids: string[]) => void
  updateDraftVerification: (verificationId: string, answer: string) => void
  saveReviewDraft: () => void
  discardReviewDraft: () => void
  clearSubmissionError: () => void
  loadAnalysisResult: (result: PropertyAnalysisResult, files: File[]) => void
  loadPropertyCosts: () => Promise<void>
  submitPropertyAndAnalyze: () => Promise<string>
}

function toCalculationPeriod(timing: CostTiming): CalculationPeriod {
  if (timing === "MONTHLY") return "MONTHLY"
  if (timing === "INITIAL") return "INITIAL"
  return "FUTURE"
}

function toDisplayCostItem(item: PropertyCostItem, sourceIndex: number, confirmations: RequiredCostConfirmation[]): CostItem {
  return {
    id: item.costItemId,
    sourceIndex,
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
    { id: "key-money", label: "레이킨(礼金)", amount: property.keyMoney, currency: "JPY", calculationPeriod: "INITIAL", originalText: null, emptyLabel: property.keyMoney === 0 ? "없음" : undefined, verifications: [] },
  ]
  const displayItems = costItems.map((item, index) => toDisplayCostItem(item, index, confirmations))

  return [
    { id: "base", title: "기본 비용", items: fixedItems },
    { id: "move-in", title: "입주 시 추가 비용", showItemCount: true, items: displayItems.filter((item) => item.calculationPeriod === "INITIAL") },
    { id: "monthly", title: "매월 추가 비용", showItemCount: true, items: displayItems.filter((item) => item.calculationPeriod === "MONTHLY") },
    { id: "renewal-exit", title: "갱신·퇴거 비용", showItemCount: true, items: displayItems.filter((item) => item.calculationPeriod === "FUTURE") },
  ]
}

function toRequiredConfirmations(result: PropertyAnalysisResult): RequiredCostConfirmation[] {
  return result.analysisDetails.costItemAnalysis
    .filter((analysis) => analysis.needsReview && result.propertyCostItems[analysis.costItemIndex] !== undefined)
    .map((analysis) => {
      const item = result.propertyCostItems[analysis.costItemIndex]
      const type: VerificationType = item.amount === null
        ? "AMOUNT"
        : item.obligationStatus === "UNKNOWN"
          ? "REQUIREDNESS"
          : item.timing === "UNKNOWN"
            ? "OCCURRENCE_TIMING"
            : "BROKER_CONFIRMATION"
      return {
        confirmationId: `analysis-review-${analysis.costItemIndex}`,
        costItemId: `analysis-cost-${analysis.costItemIndex}`,
        type,
        status: "PENDING",
        answer: null,
      }
    })
}

const timingPeriod: Record<string, CalculationPeriod> = {
  MOVE_IN: "INITIAL",
  MONTHLY: "MONTHLY",
  RENEWAL: "FUTURE",
  MOVE_OUT: "FUTURE",
}

const usePropertyCostsStore = create<PropertyCostsState>((set, get) => ({
  propertyInfo: { name: "", area: "", moveInDate: "", contractMonths: "" },
  costSections: [],
  requiredConfirmations: [],
  draftVerificationAnswers: {},
  draftSelectedCostIds: null,
  submissionStatus: "idle",
  reviewStatus: "idle",
  reviewError: null,
  savedPropertyId: null,
  analysisResult: null,
  analysisFiles: [],
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
  loadAnalysisResult: (result, files) => {
    const { property, propertyCostItems } = result
    const now = new Date().toISOString()
    const requiredConfirmations = toRequiredConfirmations(result)
    const review: PropertyCostReviewResponse = {
      property: {
        propertyId: null,
        sourceSite: property.sourceSite as PropertyCostReviewResponse["property"]["sourceSite"],
        sourceUrl: property.sourceUrl,
        name: property.propertyName?.trim() || "이름 없는 매물",
        area: [property.prefecture, property.city].filter(Boolean).join(" ") || "지역 미확인",
        rent: property.rent,
        managementFee: property.managementFee,
        deposit: property.deposit,
        keyMoney: property.keyMoney,
        availableFrom: property.availableFrom,
        contractPeriodMonths: property.contractPeriodMonths,
        listedInitialCostTotal: property.listedInitialCostTotal,
        createdAt: now,
        updatedAt: now,
      },
      costItems: propertyCostItems.map((item, index) => ({
        costItemId: `analysis-cost-${index}`,
        propertyId: null,
        ...item,
        createdAt: now,
        updatedAt: now,
      })),
      requiredConfirmations,
    }
    set({
      propertyInfo: {
        name: review.property.name,
        area: review.property.area,
        moveInDate: review.property.availableFrom ?? "",
        contractMonths: review.property.contractPeriodMonths?.toString() ?? "",
      },
      costSections: toCostSections(review, requiredConfirmations),
      requiredConfirmations,
      savedPropertyId: null,
      analysisResult: result,
      analysisFiles: files,
      reviewStatus: "success",
      reviewError: null,
      submissionStatus: "idle",
      submissionError: null,
    })
  },
  loadPropertyCosts: async () => undefined,
  submitPropertyAndAnalyze: async () => {
    let propertyId = get().savedPropertyId
    if (!propertyId) {
      set({ submissionStatus: "saving", submissionError: null })
      const { analysisResult, analysisFiles, propertyInfo, costSections } = get()

      try {
        if (!analysisResult) {
          throw new Error("분석 결과를 찾을 수 없어요. 다시 분석해주세요.")
        }
        const sourceType = analysisResult.inputType === "images" ? "IMAGE" : "URL"
        if (sourceType === "IMAGE" && (analysisFiles.length < 1 || analysisFiles.length > 3)) {
          throw new Error("분석에 사용한 이미지를 찾을 수 없어요. 다시 분석해주세요.")
        }
        const baseItems = new Map(costSections
          .find((section) => section.id === "base")
          ?.items.map((item) => [item.id, item]) ?? [])
        const editedBaseAmount = (id: string, originalAmount: number | null) => {
          const edited = baseItems.get(id)
          return edited ? edited.amount : originalAmount
        }
        const editedItems = new Map(costSections.flatMap((section) => section.items)
          .filter((item): item is CostItem & { sourceIndex: number } => item.sourceIndex !== undefined)
          .map((item) => [item.sourceIndex, item]))
        const response = await saveProperty({
          sourceType,
          modelVersion: analysisResult.modelVersion,
          property: {
            ...analysisResult.property,
            sourceSite: analysisResult.property.sourceSite ?? "UNKNOWN",
            sourceUrl: sourceType === "IMAGE" ? null : analysisResult.property.sourceUrl,
            propertyName: propertyInfo.name,
            rent: editedBaseAmount("rent", analysisResult.property.rent),
            managementFee: editedBaseAmount("management", analysisResult.property.managementFee),
            deposit: editedBaseAmount("deposit", analysisResult.property.deposit),
            keyMoney: editedBaseAmount("key-money", analysisResult.property.keyMoney),
            availableFrom: propertyInfo.moveInDate || null,
            contractPeriodMonths: propertyInfo.contractMonths ? Number(propertyInfo.contractMonths) : null,
          },
          propertyCostItems: analysisResult.propertyCostItems.map((item, index) => {
            const edited = editedItems.get(index)
            const requiredness = edited?.verifications.find((verification) => verification.type === "REQUIREDNESS" && verification.status === "RESOLVED")?.answer
            const brokerConfirmation = edited?.verifications.find((verification) => verification.type === "BROKER_CONFIRMATION" && verification.status === "RESOLVED")?.answer
            const timingAnswer = edited?.verifications.find((verification) => verification.type === "OCCURRENCE_TIMING" && verification.status === "RESOLVED")?.answer
            const obligationStatus = requiredness === "REQUIRED" || requiredness === "OPTIONAL" || requiredness === "UNKNOWN"
              ? requiredness
              : brokerConfirmation === "DUPLICATE"
                ? "OPTIONAL"
                : brokerConfirmation === "UNKNOWN"
                  ? "UNKNOWN"
                  : item.obligationStatus
            const timing = timingAnswer === "MOVE_IN"
              ? "INITIAL"
              : timingAnswer === "MONTHLY"
                ? "MONTHLY"
                : timingAnswer === "RENEWAL"
                  ? "RENEWAL"
                  : timingAnswer === "MOVE_OUT"
                    ? "MOVE_OUT"
                    : item.timing
            return {
              ...item,
              amount: edited ? edited.amount : item.amount,
              obligationStatus,
              timing,
              includedInCalculation: obligationStatus === "REQUIRED" || (obligationStatus === "OPTIONAL" && (edited?.selected ?? false)),
            }
          }),
          rawResult: analysisResult.rawResult,
        }, analysisFiles)
        propertyId = String(response.propertyId)
        set({
          savedPropertyId: propertyId,
          analysisResult: null,
          analysisFiles: [],
        })
      } catch (error) {
        set((state) => ({
          submissionStatus: "saveError",
          submissionError: {
            id: (state.submissionError?.id ?? 0) + 1,
            stage: "save",
            message: error instanceof Error ? error.message : "매물 저장에 실패했어요. 다시 시도해주세요.",
          },
        }))
        throw error
      }
    }

    set({ submissionStatus: "success", submissionError: null })
    return propertyId
  },
}))

export { usePropertyCostsStore }
export type { CalculationPeriod, CostItem, CostSectionData, CostVerification, PropertyInfo, SubmissionStatus, VerificationType }
