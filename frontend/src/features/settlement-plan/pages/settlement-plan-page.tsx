import type { ReactNode } from "react"
import { Check } from "lucide-react"
import { useNavigate, useParams } from "react-router-dom"

import { CurrencyField } from "@/features/settlement-plan/components/currency-field"
import { PlanStepLayout } from "@/features/settlement-plan/components/plan-step-layout"
import {
  additionalCostItems,
  monthlyCostItems,
  stayMonthOptions,
} from "@/features/settlement-plan/settlement-plan-data"
import { useSettlementPlanStore } from "@/features/settlement-plan/store/settlement-plan-store"
import { parseAmount } from "@/features/settlement-plan/utils/amount"
import { cn } from "@/lib/utils"

const TOTAL_STEPS = 7

function getDateDisplay(value: string) {
  const [year, month, day] = value.split("-").map(Number)

  if (!year || !month || !day) {
    return { date: "날짜를 선택해주세요", weekday: "" }
  }

  const weekday = new Intl.DateTimeFormat("ko-KR", {
    weekday: "long",
  }).format(new Date(year, month - 1, day))

  return {
    date: `${year}. ${String(month).padStart(2, "0")}. ${String(day).padStart(2, "0")}`,
    weekday,
  }
}

function StepIntro({ title, description }: { title: string; description: string }) {
  return (
    <>
      <h1 className="text-[18px] leading-7 font-bold tracking-[-0.025em]">{title}</h1>
      <p className="mt-1 text-xs leading-5 text-[var(--text-secondary)]">{description}</p>
    </>
  )
}

function FormCard({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div
      className={cn(
        "mt-6 rounded-xl bg-white px-4 py-4 shadow-[0_1px_2px_rgb(15_23_42/0.04)]",
        className,
      )}
    >
      {children}
    </div>
  )
}

function MoveInDateStep() {
  const moveInDate = useSettlementPlanStore((state) => state.moveInDate)
  const setMoveInDate = useSettlementPlanStore((state) => state.setMoveInDate)
  const display = getDateDisplay(moveInDate)

  return (
    <>
      <StepIntro
        title="일본 생활은 언제 시작할 예정인가요?"
        description="입주 예정일을 기준으로 필요한 준비기간을 계산해요."
      />

      <label className="relative mt-6 block cursor-pointer rounded-xl bg-white px-4 pt-4 pb-5 shadow-[0_1px_2px_rgb(15_23_42/0.04)] outline-none focus-within:ring-2 focus-within:ring-[var(--brand)]/25">
        <span className="block text-xs font-bold text-[var(--brand)]">입주 예정일</span>
        <span className="mt-3 flex items-end justify-between gap-3 border-b-2 border-[var(--brand)] pb-3">
          <span className="text-[22px] leading-none font-bold tracking-[-0.025em] tabular-nums">
            {display.date}
          </span>
          <span className="shrink-0 text-sm font-semibold text-[var(--text-secondary)]">
            {display.weekday}
          </span>
        </span>
        <input
          type="date"
          value={moveInDate}
          onChange={(event) => setMoveInDate(event.target.value)}
          aria-label="입주 예정일"
          className="absolute inset-0 size-full cursor-pointer opacity-0"
        />
      </label>

      <p className="mt-6 text-xs leading-5 text-[var(--text-secondary)]">
        날짜를 누르면 달력에서 변경할 수 있어요.
      </p>
    </>
  )
}

function StayDurationStep() {
  const stayMonths = useSettlementPlanStore((state) => state.stayMonths)
  const setStayMonths = useSettlementPlanStore((state) => state.setStayMonths)

  return (
    <>
      <StepIntro
        title="일본에서 얼마나 머물 예정인가요?"
        description="예상 체류기간은 1개월부터 24개월까지 선택할 수 있어요."
      />
      <FormCard>
        <p className="text-xs font-bold text-[var(--brand)]">예상 체류기간</p>
        <div className="mt-3 flex items-end gap-3 border-b-2 border-[var(--brand)] pb-3">
          <span className="flex-1 text-[22px] leading-none font-bold tabular-nums">{stayMonths}</span>
          <span className="text-sm font-semibold text-[var(--text-secondary)]">개월</span>
        </div>
      </FormCard>
      <div className="mt-5 grid grid-cols-4 gap-2" aria-label="체류기간 빠른 선택">
        {stayMonthOptions.map((months) => (
          <button
            key={months}
            type="button"
            onClick={() => setStayMonths(months)}
            aria-pressed={stayMonths === months}
            className={cn(
              "min-h-11 rounded-full border bg-white text-xs font-semibold transition-colors",
              stayMonths === months
                ? "border-[var(--brand)] bg-[var(--brand-soft)] text-[var(--brand)]"
                : "border-[#e2e6e9] text-[var(--text-secondary)]",
            )}
          >
            {months}개월
          </button>
        ))}
      </div>
    </>
  )
}

function AvailableFundsStep() {
  const availableKrw = useSettlementPlanStore((state) => state.availableKrw)
  const availableJpy = useSettlementPlanStore((state) => state.availableJpy)
  const setAvailableKrw = useSettlementPlanStore((state) => state.setAvailableKrw)
  const setAvailableJpy = useSettlementPlanStore((state) => state.setAvailableJpy)

  return (
    <>
      <StepIntro
        title="현재 준비한 자금은 얼마인가요?"
        description="원화와 보유 엔화를 각각 입력해주세요."
      />
      <FormCard className="space-y-5">
        <CurrencyField
          label="원화 준비자금"
          value={availableKrw}
          currency="KRW"
          onChange={setAvailableKrw}
        />
        <CurrencyField
          label="보유 엔화"
          value={availableJpy}
          currency="JPY"
          onChange={setAvailableJpy}
        />
      </FormCard>
      <p className="mt-6 text-xs leading-5 text-[var(--text-secondary)]">
        입력한 원화는 선택한 환율을 기준으로 환산됩니다.
      </p>
    </>
  )
}

function EmergencyFundsStep() {
  const emergencyKrw = useSettlementPlanStore((state) => state.emergencyKrw)
  const emergencyJpy = useSettlementPlanStore((state) => state.emergencyJpy)
  const setEmergencyKrw = useSettlementPlanStore((state) => state.setEmergencyKrw)
  const setEmergencyJpy = useSettlementPlanStore((state) => state.setEmergencyJpy)

  return (
    <>
      <StepIntro
        title="비상시에 남겨둘 돈은 얼마인가요?"
        description="이 금액은 생활 가능기간 계산에서 제외해요."
      />
      <FormCard className="space-y-5">
        <CurrencyField
          label="원화 비상예비비"
          value={emergencyKrw}
          currency="KRW"
          onChange={setEmergencyKrw}
        />
        <CurrencyField
          label="엔화 비상예비비"
          value={emergencyJpy}
          currency="JPY"
          onChange={setEmergencyJpy}
        />
      </FormCard>
      <p className="mt-6 text-xs leading-5 text-[var(--text-secondary)]">
        아직 정하지 않았다면 0원으로 넘어갈 수 있어요.
      </p>
    </>
  )
}

function AdditionalCostsStep() {
  const costs = useSettlementPlanStore((state) => state.additionalCosts)
  const toggleCost = useSettlementPlanStore((state) => state.toggleAdditionalCost)
  const setCostAmount = useSettlementPlanStore((state) => state.setAdditionalCostAmount)
  const total = additionalCostItems.reduce(
    (sum, item) => sum + (costs[item.key].selected ? costs[item.key].amount : 0),
    0,
  )

  return (
    <>
      <StepIntro
        title="입주 전에 추가로 드는 비용이 있나요?"
        description="필요한 항목만 선택하고 금액과 통화를 입력하세요."
      />
      <FormCard className="divide-y divide-[#edf0f2] py-1">
        {additionalCostItems.map((item) => {
          const cost = costs[item.key]
          return (
            <div key={item.key} className="py-3">
              <div className="flex min-h-11 items-center gap-3">
                <button
                  type="button"
                  onClick={() => toggleCost(item.key)}
                  className={cn(
                    "grid size-6 shrink-0 place-items-center rounded border",
                    cost.selected
                      ? "border-[var(--brand)] bg-[var(--brand)] text-white"
                      : "border-[#cfd5da] bg-white text-transparent",
                  )}
                  role="checkbox"
                  aria-checked={cost.selected}
                  aria-label={`${item.label} 선택`}
                >
                  <Check className="size-4" strokeWidth={3} />
                </button>
                <span className="flex-1 text-sm font-semibold">{item.label}</span>
                <span className="text-xs font-semibold text-[var(--text-secondary)]">JPY</span>
              </div>
              <label className="ml-9 flex min-h-10 items-center border-b border-[#d9dfe3] focus-within:border-[var(--brand)]">
                <input
                  type="text"
                  inputMode="numeric"
                  value={cost.selected ? cost.amount.toLocaleString("ko-KR") : ""}
                  onChange={(event) => setCostAmount(item.key, parseAmount(event.target.value))}
                  disabled={!cost.selected}
                  placeholder={item.placeholder}
                  aria-label={`${item.label} 금액`}
                  className="min-w-0 flex-1 bg-transparent text-right text-sm font-semibold tabular-nums outline-none placeholder:font-normal placeholder:text-[#b7bdc4] disabled:cursor-not-allowed"
                />
              </label>
            </div>
          )
        })}
        <div className="flex items-center justify-end py-4 text-[20px] font-bold text-[var(--brand)] tabular-nums">
          ¥{total.toLocaleString("ko-KR")}
        </div>
      </FormCard>
    </>
  )
}

function MonthlyCostsStep() {
  const costs = useSettlementPlanStore((state) => state.monthlyCosts)
  const inputMode = useSettlementPlanStore((state) => state.monthlyInputMode)
  const setMonthlyCost = useSettlementPlanStore((state) => state.setMonthlyCost)
  const setInputMode = useSettlementPlanStore((state) => state.setMonthlyInputMode)
  const applyDefaults = useSettlementPlanStore((state) => state.applyMonthlyDefaults)
  const total = monthlyCostItems.reduce((sum, item) => sum + costs[item.key], 0)

  return (
    <>
      <StepIntro
        title="한 달 생활비는 얼마로 예상하나요?"
        description="항목별 금액을 직접 입력하거나 기본값으로 시작할 수 있어요."
      />
      <div className="mt-5 grid grid-cols-2 rounded-lg bg-[#e8edee] p-1">
        <button
          type="button"
          onClick={() => setInputMode("direct")}
          aria-pressed={inputMode === "direct"}
          className={cn(
            "min-h-11 rounded-md text-sm font-semibold",
            inputMode === "direct" ? "bg-white text-[var(--brand)] shadow-sm" : "text-[#69717c]",
          )}
        >
          직접 입력
        </button>
        <button
          type="button"
          onClick={applyDefaults}
          aria-pressed={inputMode === "default"}
          className={cn(
            "min-h-11 rounded-md text-sm font-semibold",
            inputMode === "default" ? "bg-white text-[var(--brand)] shadow-sm" : "text-[#69717c]",
          )}
        >
          기본 값으로 세팅
        </button>
      </div>
      <FormCard className="mt-4 divide-y divide-[#edf0f2] py-1">
        {monthlyCostItems.map((item) => (
          <label key={item.key} className="flex min-h-[62px] items-center gap-3">
            <span className="flex-1 text-sm font-semibold">{item.label}</span>
            <input
              type="text"
              inputMode="numeric"
              value={costs[item.key].toLocaleString("ko-KR")}
              onChange={(event) => setMonthlyCost(item.key, parseAmount(event.target.value))}
              className="w-28 border-b border-[#d9dfe3] bg-transparent pb-2 text-right text-sm font-semibold tabular-nums outline-none focus:border-[var(--brand)]"
              aria-label={`${item.label} 월 금액`}
            />
            <span className="text-xs font-semibold text-[var(--text-secondary)]">JPY</span>
          </label>
        ))}
        <div className="flex items-end justify-between py-4">
          <span className="text-sm font-bold">월 생활비 합계</span>
          <span className="text-[20px] font-bold text-[var(--brand)] tabular-nums">
            ¥{total.toLocaleString("ko-KR")}
          </span>
        </div>
      </FormCard>
    </>
  )
}

function SummaryRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex gap-4 py-1.5 text-sm">
      <dt className="flex-1 text-[var(--text-secondary)]">{label}</dt>
      <dd className="font-semibold tabular-nums">{value}</dd>
    </div>
  )
}

function PlanSummaryStep() {
  const navigate = useNavigate()
  const moveInDate = useSettlementPlanStore((state) => state.moveInDate)
  const stayMonths = useSettlementPlanStore((state) => state.stayMonths)
  const availableKrw = useSettlementPlanStore((state) => state.availableKrw)
  const availableJpy = useSettlementPlanStore((state) => state.availableJpy)
  const emergencyKrw = useSettlementPlanStore((state) => state.emergencyKrw)
  const emergencyJpy = useSettlementPlanStore((state) => state.emergencyJpy)
  const additionalCosts = useSettlementPlanStore((state) => state.additionalCosts)
  const monthlyCosts = useSettlementPlanStore((state) => state.monthlyCosts)
  const monthlyInputMode = useSettlementPlanStore((state) => state.monthlyInputMode)

  const additionalTotal = additionalCostItems.reduce(
    (sum, item) => sum + (additionalCosts[item.key].selected ? additionalCosts[item.key].amount : 0),
    0,
  )
  const monthlyTotal = monthlyCostItems.reduce((sum, item) => sum + monthlyCosts[item.key], 0)
  const selectedAdditionalCount = additionalCostItems.filter(
    (item) => additionalCosts[item.key].selected,
  ).length

  return (
    <>
      <StepIntro
        title="입력한 계획을 마지막으로 확인해주세요"
        description="저장하면 홈 화면의 정착 준비 단계에 반영돼요."
      />
      <div className="mt-6 space-y-3">
        <section className="rounded-xl bg-white p-4">
          <div className="mb-2 flex items-center justify-between">
            <h2 className="text-sm font-bold">기간</h2>
            <button
              type="button"
              onClick={() => void navigate("/plan")}
              className="min-h-11 px-1 text-xs font-bold text-[var(--brand)]"
            >
              수정
            </button>
          </div>
          <dl>
            <SummaryRow label="입주 예정일" value={moveInDate.replaceAll("-", ".")} />
            <SummaryRow label="예상 체류기간" value={`${stayMonths}개월`} />
          </dl>
        </section>
        <section className="rounded-xl bg-white p-4">
          <div className="mb-2 flex items-center justify-between">
            <h2 className="text-sm font-bold">준비자금</h2>
            <button
              type="button"
              onClick={() => void navigate("/plan/3")}
              className="min-h-11 px-1 text-xs font-bold text-[var(--brand)]"
            >
              수정
            </button>
          </div>
          <dl>
            <SummaryRow label="원화 준비자금" value={`₩${availableKrw.toLocaleString("ko-KR")}`} />
            <SummaryRow label="보유 엔화" value={`¥${availableJpy.toLocaleString("ko-KR")}`} />
            <SummaryRow label="비상예비비" value={`₩${emergencyKrw.toLocaleString("ko-KR")} · ¥${emergencyJpy.toLocaleString("ko-KR")}`} />
          </dl>
        </section>
        <section className="rounded-xl bg-white p-4">
          <div className="mb-2 flex items-center justify-between">
            <h2 className="text-sm font-bold">추가 초기비용</h2>
            <button
              type="button"
              onClick={() => void navigate("/plan/5")}
              className="min-h-11 px-1 text-xs font-bold text-[var(--brand)]"
            >
              수정
            </button>
          </div>
          <dl>
            <SummaryRow label="선택 항목" value={`${selectedAdditionalCount}개`} />
            <SummaryRow label="합계" value={`¥${additionalTotal.toLocaleString("ko-KR")}`} />
          </dl>
        </section>
        <section className="rounded-xl bg-white p-4">
          <div className="mb-2 flex items-center justify-between">
            <h2 className="text-sm font-bold">월 생활비</h2>
            <button
              type="button"
              onClick={() => void navigate("/plan/6")}
              className="min-h-11 px-1 text-xs font-bold text-[var(--brand)]"
            >
              수정
            </button>
          </div>
          <dl>
            <SummaryRow label="월 생활비 합계" value={`¥${monthlyTotal.toLocaleString("ko-KR")}`} />
            <SummaryRow
              label="입력 방식"
              value={monthlyInputMode === "default" ? "기본값 적용" : "직접 입력"}
            />
          </dl>
        </section>
      </div>
    </>
  )
}

function getCurrentStep(stepParam: string | undefined) {
  if (!stepParam) return 1
  const parsed = Number(stepParam)
  return Number.isInteger(parsed) && parsed >= 2 && parsed <= TOTAL_STEPS ? parsed : 1
}

function SettlementPlanPage() {
  const navigate = useNavigate()
  const { step } = useParams()
  const currentStep = getCurrentStep(step)
  const moveInDate = useSettlementPlanStore((state) => state.moveInDate)
  const stayMonths = useSettlementPlanStore((state) => state.stayMonths)
  const availableKrw = useSettlementPlanStore((state) => state.availableKrw)
  const availableJpy = useSettlementPlanStore((state) => state.availableJpy)
  const monthlyCosts = useSettlementPlanStore((state) => state.monthlyCosts)

  const screens: Record<number, ReactNode> = {
    1: <MoveInDateStep />,
    2: <StayDurationStep />,
    3: <AvailableFundsStep />,
    4: <EmergencyFundsStep />,
    5: <AdditionalCostsStep />,
    6: <MonthlyCostsStep />,
    7: <PlanSummaryStep />,
  }

  const monthlyTotal = Object.values(monthlyCosts).reduce((sum, amount) => sum + amount, 0)
  const isNextDisabled =
    (currentStep === 1 && !moveInDate) ||
    (currentStep === 2 && stayMonths <= 0) ||
    (currentStep === 3 && availableKrw + availableJpy <= 0) ||
    (currentStep === 6 && monthlyTotal <= 0)

  const handleBack = () => {
    if (currentStep === 1) {
      void navigate("/home")
      return
    }
    void navigate(currentStep === 2 ? "/plan" : `/plan/${currentStep - 1}`)
  }

  const handleNext = () => {
    if (currentStep === TOTAL_STEPS) {
      void navigate("/home?stage=2")
      return
    }
    void navigate(`/plan/${currentStep + 1}`)
  }

  return (
    <PlanStepLayout
      currentStep={currentStep}
      totalSteps={TOTAL_STEPS}
      nextLabel={currentStep === TOTAL_STEPS ? "계획 저장하기" : currentStep === 6 ? "입력 내용 확인" : "다음"}
      nextDisabled={isNextDisabled}
      onBack={handleBack}
      onNext={handleNext}
    >
      {screens[currentStep]}
    </PlanStepLayout>
  )
}

export { SettlementPlanPage }
