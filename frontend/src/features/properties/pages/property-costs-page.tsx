import { useState, type ReactNode } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { AlertCircle, ArrowLeft, Check, ChevronRight, Info, LoaderCircle, X } from "lucide-react"
import { Controller, useForm, useWatch, type UseFormRegisterReturn } from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { z } from "zod"

import { ErrorToast } from "@/components/ui/error-toast"
import { usePropertyCostsStore } from "@/features/properties/store/property-costs-store"
import type { CostItem, CostSectionData, CostVerification, PropertyInfo, VerificationType } from "@/features/properties/store/property-costs-store"
import { cn } from "@/lib/utils"

const propertyInfoSchema = z.object({
  name: z.string().trim().min(1, "매물명을 입력해주세요."),
  area: z.string().trim().min(1, "지역을 입력해주세요."),
  moveInDate: z.string().min(1, "입주 가능일을 선택해주세요."),
  contractMonths: z.string().regex(/^\d+$/, "계약기간을 숫자로 입력해주세요."),
})

const costAmountsSchema = z.object({
  amounts: z.record(z.string(), z.string().regex(/^\d*$/, "금액은 숫자로 입력해주세요.")),
})

const amountVerificationSchema = z.object({
  answer: z.string().regex(/^\d+$/, "금액을 입력해주세요.").refine((value) => Number(value) > 0, "0보다 큰 금액을 입력해주세요."),
})

const choiceVerificationSchema = z.object({
  answer: z.string().min(1, "확인한 값을 선택해주세요."),
})

const selectionCostsSchema = z.object({ selectedIds: z.array(z.string()) })

type CostAmountsFormValues = z.infer<typeof costAmountsSchema>
type VerificationFormValues = z.infer<typeof choiceVerificationSchema>
type SelectionCostsFormValues = z.infer<typeof selectionCostsSchema>

function CostsHeader({ title, onBack }: { title: string; onBack?: () => void }) {
  const navigate = useNavigate()

  return (
    <header className="sticky top-0 z-20 bg-white pt-[env(safe-area-inset-top)]">
      <div className="grid h-14 grid-cols-[44px_1fr_44px] items-center px-2">
        <button
          type="button"
          onClick={onBack ?? (() => void navigate(-1))}
          className="grid size-11 place-items-center rounded-full"
          aria-label="뒤로 가기"
        >
          <ArrowLeft aria-hidden="true" className="size-5" />
        </button>
        <h1 className="text-center text-sm font-bold">{title}</h1>
      </div>
    </header>
  )
}

function CostSection({ section, disabled = false, onEdit }: { section: CostSectionData; disabled?: boolean; onEdit?: () => void }) {
  return (
    <section className="border-b-8 border-[#f5f6f7] bg-white px-4 py-5">
      <div className="relative">
        <h2 className="text-base font-bold">{section.title}{section.showItemCount ? ` (${section.items.length})` : ""}</h2>
        <button
          type="button"
          disabled={disabled}
          onClick={onEdit}
          className="absolute top-1/2 right-0 min-h-11 -translate-y-1/2 px-1 text-xs font-bold text-[var(--brand)] disabled:cursor-not-allowed disabled:text-[var(--text-secondary)]"
        >
          수정 <ChevronRight aria-hidden="true" className="inline size-3" />
        </button>
      </div>
      <dl className="mt-3 space-y-3">
        {section.items.map((item) => {
          const badge = getCostItemBadge(item)
          return (
          <div key={item.id} className="flex items-center justify-between gap-4 text-xs">
            <dt className="flex items-center gap-2 text-[var(--text-secondary)]">
              {item.label}
              {badge && (
                <span
                  className={cn(
                    "text-[10px] font-bold",
                    badge === "확인 필요"
                      ? "text-[#ff705d]"
                      : badge === "선택" || badge === "조건부"
                        ? "text-[#c28b36]"
                        : "text-[var(--brand)]",
                  )}
                >
                  {badge}
                </span>
              )}
            </dt>
            <dd className="shrink-0 font-semibold tabular-nums">{formatCostItemValue(item)}</dd>
          </div>
        )})}
      </dl>
    </section>
  )
}

function BottomSheet({ children, onClose, showClose = false }: { children: ReactNode; onClose: () => void; showClose?: boolean }) {
  return (
    <div className="bottom-sheet-backdrop fixed inset-0 z-50 flex items-end justify-center bg-black/30" role="presentation" onMouseDown={onClose}>
      <section
        role="dialog"
        aria-modal="true"
        className="bottom-sheet-panel relative max-h-[88dvh] w-full max-w-[430px] overflow-y-auto rounded-t-3xl bg-white px-5 pt-3 pb-[calc(18px+env(safe-area-inset-bottom))] shadow-2xl"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="mx-auto mb-5 h-1 w-10 rounded-full bg-[#dde2e5]" />
        <button
          type="button"
          onClick={onClose}
          className={showClose ? "absolute top-11 right-4 grid size-11 place-items-center text-[var(--text-secondary)]" : "sr-only"}
          aria-label="닫기"
        >
          {showClose && <X aria-hidden="true" className="size-6" strokeWidth={1.8} />}
        </button>
        {children}
      </section>
    </div>
  )
}

function PropertyInfoSheet({ initialValues, onApply, onClose }: { initialValues: PropertyInfo; onApply: (values: PropertyInfo) => void; onClose: () => void }) {
  const {
    control,
    formState: { errors, isValid },
    handleSubmit,
    register,
  } = useForm<PropertyInfo>({
    resolver: zodResolver(propertyInfoSchema),
    defaultValues: initialValues,
    mode: "onChange",
  })

  return (
    <BottomSheet onClose={onClose}>
      <form onSubmit={handleSubmit(onApply)} noValidate>
        <h2 className="text-lg font-bold">매물 기본정보 수정</h2>
        <p className="mt-1 text-xs text-[var(--text-secondary)]">분석에 사용할 매물 정보를 확인해주세요.</p>
        <div className="mt-5 space-y-4">
          <SheetTextField label="매물명" error={errors.name?.message} registration={register("name")} />
          <SheetTextField label="지역" error={errors.area?.message} registration={register("area")} />
          <label className="block">
            <span className="text-xs font-bold">입주 가능일</span>
            <input
              type="date"
              {...register("moveInDate")}
              className="mt-2 h-12 w-full rounded-lg border border-[#dce1e4] bg-white px-3 text-sm outline-none focus:border-[var(--brand)]"
            />
            {errors.moveInDate && <span className="mt-1 block text-[10px] text-[#ff705d]">{errors.moveInDate.message}</span>}
          </label>
          <label className="block">
            <span className="text-xs font-bold">계약기간</span>
            <span className="mt-2 flex h-12 items-center rounded-lg border border-[#dce1e4] px-3 focus-within:border-[var(--brand)]">
              <Controller
                name="contractMonths"
                control={control}
                render={({ field }) => (
                  <input
                    type="text"
                    inputMode="numeric"
                    value={field.value}
                    onBlur={field.onBlur}
                    onChange={(event) => field.onChange(event.target.value.replace(/\D/g, ""))}
                    ref={field.ref}
                    className="min-w-0 flex-1 bg-transparent text-sm tabular-nums outline-none"
                  />
                )}
              />
              <span className="text-xs text-[var(--text-secondary)]">개월</span>
            </span>
            {errors.contractMonths && <span className="mt-1 block text-[10px] text-[#ff705d]">{errors.contractMonths.message}</span>}
          </label>
        </div>
        <button
          type="submit"
          disabled={!isValid}
          className="mt-7 h-12 w-full rounded-lg bg-[var(--brand)] text-sm font-bold text-white disabled:cursor-not-allowed disabled:bg-[#d9dfe4]"
        >
          수정 내용 적용
        </button>
      </form>
    </BottomSheet>
  )
}

function SheetTextField({ label, error, registration }: { label: string; error?: string; registration: UseFormRegisterReturn }) {
  return (
    <label className="block">
      <span className="text-xs font-bold">{label}</span>
      <input
        type="text"
        {...registration}
        className="mt-2 h-12 w-full rounded-lg border border-[#dce1e4] bg-white px-3 text-sm outline-none focus:border-[var(--brand)]"
      />
      {error && <span className="mt-1 block text-[10px] text-[#ff705d]">{error}</span>}
    </label>
  )
}

function EditableCostsSheet({ section, onApply, onClose }: { section: CostSectionData; onApply: (values: Record<string, string>) => void; onClose: () => void }) {
  const { control, formState: { isValid }, handleSubmit } = useForm<CostAmountsFormValues>({
    resolver: zodResolver(costAmountsSchema),
    defaultValues: { amounts: Object.fromEntries(section.items.map((item) => [item.id, item.amount?.toString() ?? ""])) },
    mode: "onChange",
  })

  return (
    <BottomSheet onClose={onClose} showClose>
      <form className="flex min-h-[76dvh] flex-col" onSubmit={handleSubmit(({ amounts }) => onApply(amounts))} noValidate>
        <h2 className="pr-12 text-lg font-bold">{section.title}</h2>
        <p className="mt-1 text-xs text-[var(--text-secondary)]">AI가 찾은 값을 확인하고 필요한 항목만 수정해주세요.</p>
        <div className="mt-5 space-y-1">
          {section.items.map((item) => {
            const editStatus = getEditableCostStatus(item)

            return (
            <label key={item.id} className="flex min-h-14 items-center justify-between gap-4">
              <span className="flex min-w-0 items-center gap-2">
                <span className="text-sm font-semibold">{item.label}</span>
                {editStatus && (
                  <span
                    className={cn(
                      "shrink-0 rounded-full px-2.5 py-1 text-[10px] font-bold",
                      editStatus.tone === "warning"
                        ? "bg-[#fff3d6] text-[#d58a13]"
                        : "bg-[#eef1f4] text-[#7f8998]",
                    )}
                  >
                    {editStatus.label}
                  </span>
                )}
              </span>
              <span className="flex h-10 w-32 shrink-0 items-center border-b border-[#dce1e4] focus-within:border-[var(--brand)]">
                <Controller
                  name={`amounts.${item.id}`}
                  control={control}
                  render={({ field }) => (
                    <input
                      aria-label={`${item.label} 금액`}
                      type="text"
                      inputMode="numeric"
                      value={formatNumericInput(field.value)}
                      placeholder="금액 입력"
                      onBlur={field.onBlur}
                      onChange={(event) => field.onChange(event.target.value.replace(/\D/g, ""))}
                      ref={field.ref}
                      className="min-w-0 flex-1 bg-transparent text-right text-sm font-semibold tabular-nums outline-none placeholder:font-normal placeholder:text-[var(--text-secondary)]"
                    />
                  )}
                />
              </span>
            </label>
          )})}
        </div>
        <button
          type="submit"
          disabled={!isValid}
          className="mt-auto h-14 w-full rounded-xl bg-[var(--brand)] text-base font-bold text-white disabled:cursor-not-allowed disabled:bg-[#d9dfe4]"
        >
          변경사항 저장
        </button>
      </form>
    </BottomSheet>
  )
}

function ChoiceButton({ selected, children, onClick }: { selected: boolean; children: ReactNode; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={selected}
      className={cn(
        "min-h-12 rounded-lg border text-sm font-semibold",
        selected ? "border-[var(--brand)] bg-[var(--brand-soft)] text-[var(--brand)]" : "border-[#dce1e4] bg-white",
      )}
    >
      {children}
    </button>
  )
}

function SheetActions({ applyDisabled = false, onApply }: { applyDisabled?: boolean; onApply: () => void }) {
  return (
    <div className="mt-7">
      <button
        type="button"
        disabled={applyDisabled}
        onClick={onApply}
        className="h-12 w-full rounded-lg bg-[var(--brand)] text-sm font-bold text-white disabled:cursor-not-allowed disabled:bg-[#d9dfe4]"
      >
        확인한 값 적용
      </button>
    </div>
  )
}

type VerificationChoice = { value: string; label: string }

const verificationPresentation: Record<VerificationType, { detail: string; badge: string; subtitle: string; question: string; choices: VerificationChoice[]; apply: string; helper?: string }> = {
  AMOUNT: { detail: "금액이 표시되지 않았어요", badge: "금액 확인", subtitle: "필요한 정보 · 금액", question: "확인한 금액", choices: [], apply: "확인한 값 적용", helper: "중개업체나 매물 상세에서 확인한 금액을 입력해주세요." },
  OCCURRENCE_TIMING: { detail: "발생 시점을 확인해요", badge: "시점 확인", subtitle: "필요한 정보 · 발생 시점", question: "언제 발생하는 비용인가요?", choices: [{ value: "MOVE_IN", label: "입주" }, { value: "MONTHLY", label: "매월" }, { value: "RENEWAL", label: "갱신" }, { value: "MOVE_OUT", label: "퇴거" }], apply: "발생 시점 적용", helper: "선택한 시점에 맞춰 비용 요약과 계산에 반영해요." },
  REQUIREDNESS: { detail: "필수인지 선택인지 확인해요", badge: "필수 확인", subtitle: "필요한 정보 · 필수 여부", question: "이 비용은 계약에 필수인가요?", choices: [{ value: "REQUIRED", label: "필수" }, { value: "OPTIONAL", label: "선택" }, { value: "UNKNOWN", label: "모름" }], apply: "선택한 상태 적용", helper: "‘필수’만 초기 정착비에 바로 포함해요." },
  BROKER_CONFIRMATION: { detail: "중개업체에 비용 용도를 확인해요", badge: "업체 확인", subtitle: "필요한 정보 · 용도 및 중복", question: "확인 결과", choices: [{ value: "SEPARATE", label: "별도 비용" }, { value: "DUPLICATE", label: "중복 비용" }, { value: "UNKNOWN", label: "모름" }], apply: "확인 결과 적용" },
}

function VerificationSheet({ item, verification, initialAnswer = "", onClose, onApply }: { item: CostItem; verification: CostVerification; initialAnswer?: string; onApply: (answer: string) => void; onClose: () => void }) {
  const config = verificationPresentation[verification.type]
  const {
    control,
    formState: { isValid },
    handleSubmit,
    setValue,
  } = useForm<VerificationFormValues>({
    resolver: zodResolver(verification.type === "AMOUNT" ? amountVerificationSchema : choiceVerificationSchema),
    defaultValues: { answer: initialAnswer },
    mode: "onChange",
  })
  const answer = useWatch({ control, name: "answer" })

  if (verification.type === "AMOUNT") {
    return (
      <BottomSheet onClose={onClose}>
        <form onSubmit={handleSubmit(({ answer: value }) => onApply(value))} noValidate>
          <h2 className="text-lg font-bold">{item.label} 확인</h2>
          <p className="mt-1 text-xs text-[var(--text-secondary)]">{config.subtitle}</p>
          <p className="mt-5 text-xs text-[var(--text-secondary)]">매물 원문</p>
          <div className="mt-2 rounded-lg bg-[#f5f6f7] p-4 text-sm">{item.originalText ?? "원문 정보가 없어요."}</div>
          <label className="mt-6 block">
            <span className="text-sm font-bold">{config.question}</span>
            <span className="mt-3 flex items-center border-b border-[#dce1e4] pb-3 focus-within:border-[var(--brand)]">
              <Controller
                name="answer"
                control={control}
                render={({ field }) => (
                  <input
                    type="text"
                    inputMode="numeric"
                    value={field.value}
                    onBlur={field.onBlur}
                    onChange={(event) => field.onChange(event.target.value.replace(/\D/g, ""))}
                    ref={field.ref}
                    placeholder="예) 20,000"
                    className="min-w-0 flex-1 bg-transparent text-xl tabular-nums outline-none placeholder:text-[#aab2bb]"
                  />
                )}
              />
              <span className="text-xs text-[var(--text-secondary)]">JPY</span>
            </span>
          </label>
          <p className="mt-4 text-xs text-[var(--text-secondary)]">{config.helper}</p>
          <SheetActions applyDisabled={!isValid} onApply={handleSubmit(({ answer: value }) => onApply(value))} />
        </form>
      </BottomSheet>
    )
  }

  return (
    <BottomSheet onClose={onClose}>
      <form onSubmit={handleSubmit(({ answer: value }) => onApply(value))} noValidate>
        <h2 className="text-lg font-bold">{item.label} 확인</h2>
        <p className="mt-1 text-xs text-[var(--text-secondary)]">{config.subtitle}</p>
        <p className="mt-5 text-xs text-[var(--text-secondary)]">매물 원문</p>
        <div className="mt-2 rounded-lg bg-[#f5f6f7] p-4 text-sm">{item.originalText ?? "원문 정보가 없어요."}</div>
        {verification.type === "BROKER_CONFIRMATION" && (
          <div className="mt-4 flex gap-3 rounded-lg bg-[#15171c] p-4 text-white">
            <Info aria-hidden="true" className="mt-0.5 size-5 shrink-0 text-[var(--brand)]" />
            <div>
              <strong className="text-xs">중개업체에 확인해주세요</strong>
              <p className="mt-1 text-[11px] text-white/70">중개 수수료와 별도로 내는 비용인지 확인이 필요해요.</p>
            </div>
          </div>
        )}
        <h3 className="mt-6 text-sm font-bold">{config.question}</h3>
        <div className={cn("mt-3 grid gap-2", config.choices.length === 4 ? "grid-cols-4" : "grid-cols-3")}>
          {config.choices.map((choice) => (
            <ChoiceButton
              key={choice.value}
              selected={answer === choice.value}
              onClick={() => setValue("answer", choice.value, { shouldDirty: true, shouldValidate: true })}
            >
              {choice.label}
            </ChoiceButton>
          ))}
        </div>
        {config.helper && <p className="mt-4 text-xs text-[var(--text-secondary)]">{config.helper}</p>}
        <div className="mt-7">
          <button
            type="submit"
            disabled={!isValid}
            className="h-12 w-full rounded-lg bg-[var(--brand)] text-sm font-bold text-white disabled:cursor-not-allowed disabled:bg-[#d9dfe4]"
          >
            {config.apply}
          </button>
        </div>
      </form>
    </BottomSheet>
  )
}

function SelectionCostsSheet({ items, initialSelectedIds, onApply, onClose }: { items: CostItem[]; initialSelectedIds: string[]; onApply: (ids: string[]) => void; onClose: () => void }) {
  const { control, handleSubmit, setValue } = useForm<SelectionCostsFormValues>({
    resolver: zodResolver(selectionCostsSchema),
    defaultValues: { selectedIds: initialSelectedIds },
  })
  const selectedIds = useWatch({ control, name: "selectedIds" })

  return (
    <BottomSheet onClose={onClose}>
      <form onSubmit={handleSubmit(({ selectedIds: ids }) => onApply(ids))} noValidate>
        <h2 className="text-lg font-bold">선택 비용</h2>
        <p className="mt-1 text-xs text-[var(--text-secondary)]">필요한 서비스만 골라주세요.</p>
        <div className="mt-4 divide-y divide-[#e6eaed]">
          {items.map((item) => (
            <label key={item.id} className="flex min-h-[68px] cursor-pointer items-center justify-between gap-4">
              <span>
                <span className="block text-sm font-bold">{item.label}</span>
                <span className="mt-1 block text-xs text-[var(--text-secondary)]">
                  {formatCostItemValue(item)}{item.calculationPeriod === "INITIAL" ? " · 초기 1회" : ""}
                </span>
              </span>
              <input
                type="checkbox"
                checked={selectedIds.includes(item.id)}
                onChange={() => setValue(
                  "selectedIds",
                  selectedIds.includes(item.id) ? selectedIds.filter((id) => id !== item.id) : [...selectedIds, item.id],
                  { shouldDirty: true, shouldValidate: true },
                )}
                className="peer sr-only"
              />
              <span className="relative h-8 w-12 shrink-0 rounded-full bg-[#e9edf0] transition-colors peer-checked:bg-[var(--brand)] after:absolute after:top-1 after:left-1 after:size-6 after:rounded-full after:bg-white after:shadow-sm after:transition-transform peer-checked:after:translate-x-4" />
            </label>
          ))}
        </div>
        <p className="mt-5 text-xs text-[var(--text-secondary)]">선택 비용은 언제든 다시 바꿀 수 있어요.</p>
        <button type="submit" className="mt-7 h-12 w-full rounded-lg bg-[var(--brand)] text-sm font-bold text-white">
          선택한 비용 적용
        </button>
      </form>
    </BottomSheet>
  )
}

function formatCostItemValue(item: CostItem) {
  if (item.amount === null) return item.monthly ? `${item.emptyLabel ?? "-"} /월` : item.emptyLabel ?? "-"
  if (item.amount === 0 && item.emptyLabel) return item.monthly ? `${item.emptyLabel} /월` : item.emptyLabel
  return `¥${item.amount.toLocaleString("en-US")}${item.monthly ? " /월" : ""}`
}

function formatNumericInput(value: string) {
  return value ? Number(value).toLocaleString("en-US") : ""
}

function getEditableCostStatus(item: CostItem) {
  if (item.amount === null || item.verifications.some((verification) => verification.type === "AMOUNT" && verification.status === "PENDING")) {
    return { label: "미표기", tone: "neutral" as const }
  }
  if (item.verifications.some((verification) => verification.status === "PENDING")) {
    return { label: "미확인", tone: "warning" as const }
  }
  return undefined
}

function formatVerificationAnswer(type: VerificationType, answer: string) {
  if (type === "AMOUNT") return `¥${Number(answer).toLocaleString("en-US")}`
  return verificationPresentation[type].choices.find((choice) => choice.value === answer)?.label ?? answer
}

function getCostItemBadge(item: CostItem) {
  if (item.verifications.some((verification) => verification.status === "PENDING")) return "확인 필요"
  const resolvedChoice = item.verifications.find((verification) => verification.type !== "AMOUNT" && verification.answer)
  if (resolvedChoice?.answer) return formatVerificationAnswer(resolvedChoice.type, resolvedChoice.answer)
  if (item.selectable) return "선택"
  if (item.conditional) return "조건부"
  return undefined
}

function PropertyCostsPage() {
  const navigate = useNavigate()
  const [editSheet, setEditSheet] = useState<"property" | string | null>(null)
  const propertyInfo = usePropertyCostsStore((state) => state.propertyInfo)
  const costSections = usePropertyCostsStore((state) => state.costSections)
  const requiredConfirmations = usePropertyCostsStore((state) => state.requiredConfirmations)
  const updatePropertyInfo = usePropertyCostsStore((state) => state.updatePropertyInfo)
  const updateCostAmounts = usePropertyCostsStore((state) => state.updateCostAmounts)
  const submissionStatus = usePropertyCostsStore((state) => state.submissionStatus)
  const submissionError = usePropertyCostsStore((state) => state.submissionError)
  const clearSubmissionError = usePropertyCostsStore((state) => state.clearSubmissionError)
  const reviewStatus = usePropertyCostsStore((state) => state.reviewStatus)
  const submitPropertyAndAnalyze = usePropertyCostsStore((state) => state.submitPropertyAndAnalyze)

  if (reviewStatus !== "success") {
    return (
      <main className="min-h-dvh bg-[#f5f6f7]">
        <CostsHeader title="비용 확인" />
        <section className="flex min-h-[calc(100dvh-64px)] flex-col items-center justify-center px-6 text-center" role="alert">
          <AlertCircle aria-hidden="true" className="size-9 text-[#ff705d]" />
          <h2 className="mt-4 text-base font-bold">분석 결과가 없어요</h2>
          <p className="mt-2 text-xs text-[var(--text-secondary)]">매물을 등록하고 AI 분석을 완료한 뒤 확인해주세요.</p>
          <button type="button" onClick={() => void navigate("/properties/new", { replace: true })} className="mt-6 h-12 min-w-36 rounded-lg bg-[var(--brand)] px-5 text-sm font-bold text-white">
            매물 등록하기
          </button>
        </section>
      </main>
    )
  }
  const pendingCount = requiredConfirmations.filter((confirmation) => confirmation.status === "PENDING").length
  const activeEditSection = costSections.find((section) => section.id === editSheet)
  const isSubmitting = submissionStatus === "saving" || submissionStatus === "analyzing"
  const submitButtonLabel = submissionStatus === "saving"
    ? "분석 준비 중..."
    : submissionStatus === "analyzing"
      ? "비용 분석 중..."
      : "비용 분석하기"

  return (
    <main className="min-h-dvh bg-[#f5f6f7]">
      <CostsHeader title="비용 확인" />
      <div
        className="flex gap-1 bg-white px-4 pb-3"
        role="progressbar"
        aria-label="매물 등록 진행률"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={100}
      >
        <span className="h-1 flex-1 rounded-full bg-[var(--brand)]" />
        <span className="h-1 flex-1 rounded-full bg-[var(--brand)]" />
      </div>

      <section className="px-4 pt-5 pb-4">
        <div className="mb-2 flex items-center gap-1.5 text-[11px] font-bold text-[var(--brand)]">
          <span className="grid size-5 place-items-center rounded-full bg-[var(--brand-soft)]">
            <Check aria-hidden="true" className="size-3" strokeWidth={3} />
          </span>
          추출 완료
        </div>
        <h2 className="text-lg font-bold">AI 추출 결과를 확인해주세요</h2>
        <p className="mt-1 text-xs text-[var(--text-secondary)]">등록한 매물 정보에서 비용 항목을 정리했어요.</p>
      </section>

      {pendingCount > 0 && (
        <div className="bg-white">
          <button
            type="button"
            disabled={isSubmitting}
            onClick={() => void navigate("/properties/costs/review")}
            className="flex min-h-[72px] w-full items-center gap-3 bg-[#15171c] px-4 text-left text-white disabled:cursor-not-allowed disabled:opacity-60"
          >
            <AlertCircle aria-hidden="true" className="size-5 shrink-0 text-[var(--brand)]" />
            <span className="min-w-0 flex-1">
              <span className="block text-xs font-bold">비용 확인이 필요해요</span>
              <span className="mt-1 block text-[10px] text-white/65">{pendingCount}개 항목은 확인 전까지 계산에서 제외돼요.</span>
            </span>
            <ChevronRight aria-hidden="true" className="size-5 shrink-0" />
          </button>
        </div>
      )}

      <section className="border-b-8 border-[#f5f6f7] bg-white px-4 py-5">
        <div className="relative">
          <h2 className="text-base font-bold">매물 기본정보</h2>
            <button
              type="button"
              disabled={isSubmitting}
              onClick={() => setEditSheet("property")}
              className="absolute top-1/2 right-0 min-h-11 -translate-y-1/2 px-1 text-xs font-bold text-[var(--brand)] disabled:cursor-not-allowed disabled:text-[var(--text-secondary)]"
          >
            수정 <ChevronRight className="inline size-3" />
          </button>
        </div>
        <dl className="mt-3 space-y-3 text-xs">
          <div className="flex justify-between"><dt className="text-[var(--text-secondary)]">매물명</dt><dd className="font-semibold">{propertyInfo.name}</dd></div>
          <div className="flex justify-between"><dt className="text-[var(--text-secondary)]">지역</dt><dd className="font-semibold">{propertyInfo.area}</dd></div>
          <div className="flex justify-between"><dt className="text-[var(--text-secondary)]">입주 가능일</dt><dd className="font-semibold">{propertyInfo.moveInDate.replaceAll("-", ".")}</dd></div>
          <div className="flex justify-between"><dt className="text-[var(--text-secondary)]">계약기간</dt><dd className="font-semibold">{propertyInfo.contractMonths}개월</dd></div>
        </dl>
      </section>

      {costSections.map((section) => (
        <CostSection
          key={section.id}
          section={section}
          disabled={isSubmitting}
          onEdit={() => setEditSheet(section.id)}
        />
      ))}

      <section className="relative bg-[#f5f6f7] px-4 pt-6 pb-[calc(18px+env(safe-area-inset-bottom))]">
        {submissionError && (
          <ErrorToast key={submissionError.id} message={submissionError.message} onDismiss={clearSubmissionError} />
        )}
        <button
          type="button"
          disabled={isSubmitting || pendingCount > 0}
          aria-busy={isSubmitting}
          onClick={() => {
            void submitPropertyAndAnalyze()
              .then((propertyId) => void navigate(`/properties/${propertyId}`, { replace: true }))
              .catch(() => undefined)
          }}
          className="flex h-14 w-full items-center justify-center gap-2 rounded-xl bg-[var(--brand)] text-base font-bold text-white disabled:cursor-not-allowed disabled:bg-[#d9dfe4]"
        >
          {isSubmitting && <LoaderCircle aria-hidden="true" className="size-5 animate-spin" />}
          {submitButtonLabel}
        </button>
        {isSubmitting && (
          <p className="mt-3 text-center text-xs text-[var(--text-secondary)]" role="status" aria-live="polite">
            확인한 비용을 바탕으로 초기비용과 월 주거비를 분석하고 있어요.
          </p>
        )}
      </section>

      {editSheet === "property" && (
        <PropertyInfoSheet
          initialValues={propertyInfo}
          onClose={() => setEditSheet(null)}
          onApply={(values) => {
            updatePropertyInfo(values)
            setEditSheet(null)
          }}
        />
      )}
      {activeEditSection && (
        <EditableCostsSheet
          section={activeEditSection}
          onClose={() => setEditSheet(null)}
          onApply={(values) => {
            updateCostAmounts(values)
            setEditSheet(null)
          }}
        />
      )}
    </main>
  )
}

function RequiredCostsPage() {
  const navigate = useNavigate()
  const [activeVerificationId, setActiveVerificationId] = useState<string | null>(null)
  const [selectionOpen, setSelectionOpen] = useState(false)
  const costSections = usePropertyCostsStore((state) => state.costSections)
  const requiredConfirmations = usePropertyCostsStore((state) => state.requiredConfirmations)
  const draftVerificationAnswers = usePropertyCostsStore((state) => state.draftVerificationAnswers)
  const draftSelectedCostIds = usePropertyCostsStore((state) => state.draftSelectedCostIds)
  const updateDraftVerification = usePropertyCostsStore((state) => state.updateDraftVerification)
  const updateDraftSelectedCostIds = usePropertyCostsStore((state) => state.updateDraftSelectedCostIds)
  const saveReviewDraft = usePropertyCostsStore((state) => state.saveReviewDraft)
  const discardReviewDraft = usePropertyCostsStore((state) => state.discardReviewDraft)
  const allItems = costSections.flatMap((section) => section.items)
  const verificationTasks = requiredConfirmations
    .filter((confirmation) => confirmation.status === "PENDING")
    .flatMap((confirmation) => {
      const item = allItems.find((costItem) => costItem.id === confirmation.costItemId)
      if (!item) return []

      return [{
        item,
        verification: {
          id: confirmation.confirmationId,
          type: confirmation.type,
          status: confirmation.status,
          answer: confirmation.answer,
        } satisfies CostVerification,
      }]
    })
  const activeTask = verificationTasks.find(({ verification }) => verification.id === activeVerificationId)
  const selectableItems = allItems.filter((item) => item.selectable)
  const savedSelectedIds = selectableItems.filter((item) => item.selected).map((item) => item.id)
  const displayedSelectedIds = draftSelectedCostIds ?? savedSelectedIds
  const conditionalItems = allItems.filter((item) => item.conditional)
  const hasDraft = Object.keys(draftVerificationAnswers).length > 0 || draftSelectedCostIds !== null

  return (
    <main className="min-h-dvh bg-[#f5f6f7] pb-[calc(92px+env(safe-area-inset-bottom))]">
      <CostsHeader title="확인할 비용" onBack={() => { discardReviewDraft(); void navigate(-1) }} />
      <section className="px-4 py-6">
        <h2 className="text-xl font-bold">추가 확인이 필요한 비용</h2>
        <p className="mt-2 text-xs text-[var(--text-secondary)]">확인한 항목만 예상 비용에 반영해요.</p>
      </section>
      <section className="bg-white pt-4">
        <h3 className="px-4 pb-3 text-base font-bold">확인 필요 항목</h3>
        <div className="divide-y divide-[#edf0f2]">
          {verificationTasks.map(({ item, verification }) => {
            const config = verificationPresentation[verification.type]
            const draftAnswer = draftVerificationAnswers[verification.id]
            const isChecked = draftAnswer !== undefined

            return (
              <button key={verification.id} type="button" onClick={() => setActiveVerificationId(verification.id)} className="flex min-h-[76px] w-full items-center gap-3 bg-white px-4 text-left transition-colors duration-100 active:bg-[var(--brand-soft)]">
                <span className="min-w-0 flex-1">
                  <span className="block text-sm font-bold">{item.label}</span>
                  <span className={cn("mt-1 block text-xs", isChecked ? "font-semibold text-[var(--brand)]" : "text-[var(--text-secondary)]")}>
                    {isChecked ? formatVerificationAnswer(verification.type, draftAnswer) : config.detail}
                  </span>
                </span>
                {isChecked ? (
                  <span className="flex shrink-0 items-center gap-0.5 text-xs font-bold text-[var(--brand)]">수정 <ChevronRight aria-hidden="true" className="size-3" /></span>
                ) : (
                  <span className="shrink-0 rounded-full bg-[#ff705d] px-3 py-1 text-[10px] font-bold text-white">{config.badge}</span>
                )}
              </button>
            )
          })}
          {verificationTasks.length === 0 && (
            <div className="flex min-h-28 flex-col items-center justify-center gap-2 px-4 text-center">
              <span className="grid size-8 place-items-center rounded-full bg-[var(--brand-soft)] text-[var(--brand)]"><Check aria-hidden="true" className="size-4" strokeWidth={3} /></span>
              <p className="text-sm font-bold">모든 비용을 확인했어요</p>
            </div>
          )}
        </div>
      </section>

      {selectableItems.length > 0 && (
        <section className="mt-2 bg-white px-4 py-5">
          <div className="relative">
            <h3 className="text-base font-bold">선택 비용</h3>
            <button type="button" onClick={() => setSelectionOpen(true)} className="absolute top-1/2 right-0 min-h-11 -translate-y-1/2 px-1 text-xs font-bold text-[var(--brand)]">수정 <ChevronRight aria-hidden="true" className="inline size-3" /></button>
          </div>
          <p className="mt-1 text-xs text-[var(--text-secondary)]">원하는 서비스만 예상 비용에 포함해요.</p>
          <div className="mt-5 space-y-5">
            {selectableItems.filter((item) => displayedSelectedIds.includes(item.id)).map((item) => (
              <div key={item.id}><p className="text-sm font-bold">{item.label}</p><p className="mt-1 text-xs text-[var(--text-secondary)]">{formatCostItemValue(item)}{item.calculationPeriod === "INITIAL" ? " · 초기 1회" : ""}</p></div>
            ))}
            {!selectableItems.some((item) => displayedSelectedIds.includes(item.id)) && <p className="text-xs text-[var(--text-secondary)]">선택한 비용이 없어요.</p>}
          </div>
        </section>
      )}

      {conditionalItems.length > 0 && (
        <section className="mt-2 bg-white px-4 py-5">
          <div className="flex items-center justify-between"><div><h3 className="text-base font-bold">조건부 비용</h3><p className="mt-1 text-xs text-[var(--text-secondary)]">계약 조건이 발생할 때만 내는 비용이에요.</p></div><span className="rounded-full bg-[#737b87] px-3 py-1 text-[10px] font-bold text-white">조건부</span></div>
          <div className="mt-5 space-y-4">
            {conditionalItems.map((item) => <div key={item.id} className="flex justify-between gap-4 text-sm"><span>{item.label}</span><strong>{item.description ? `${item.description} ` : ""}{formatCostItemValue(item)}</strong></div>)}
          </div>
        </section>
      )}

      <div className="fixed inset-x-0 bottom-0 z-20 mx-auto w-full max-w-[430px] bg-[#f5f6f7] px-4 pt-4 pb-[calc(14px+env(safe-area-inset-bottom))]">
        <button type="button" disabled={!hasDraft} onClick={() => { saveReviewDraft(); void navigate("/properties/costs") }} className="h-12 w-full rounded-lg bg-[var(--brand)] text-sm font-bold text-white disabled:cursor-not-allowed disabled:bg-[#d9dfe4]">확인 내용 저장하기</button>
      </div>

      {activeTask && (
        <VerificationSheet
          item={activeTask.item}
          verification={activeTask.verification}
          initialAnswer={draftVerificationAnswers[activeTask.verification.id]}
          onClose={() => setActiveVerificationId(null)}
          onApply={(answer) => { updateDraftVerification(activeTask.verification.id, answer); setActiveVerificationId(null) }}
        />
      )}
      {selectionOpen && (
        <SelectionCostsSheet
          items={selectableItems}
          initialSelectedIds={displayedSelectedIds}
          onClose={() => setSelectionOpen(false)}
          onApply={(ids) => { updateDraftSelectedCostIds(ids); setSelectionOpen(false) }}
        />
      )}
    </main>
  )
}

export { PropertyCostsPage, RequiredCostsPage }
