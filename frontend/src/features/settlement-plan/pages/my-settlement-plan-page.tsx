import { type ReactNode } from "react"
import { useQuery } from "@tanstack/react-query"
import { ArrowLeft, ChevronRight, LoaderCircle } from "lucide-react"
import { Link, useNavigate, useSearchParams } from "react-router-dom"

import { BottomNav } from "@/components/layout/bottom-nav"
import { fetchSettlementPlan, type SettlementPlan } from "@/features/settlement-plan/api/settlement-plan-api"

function formatKrw(value: number) {
  return `₩${value.toLocaleString("ko-KR")}`
}

function formatJpy(value: number) {
  return `¥${value.toLocaleString("ko-KR")}`
}

function formatDate(value: string) {
  return value.replaceAll("-", ".")
}

function daysUntil(value: string) {
  const today = new Date("2026-09-08T00:00:00")
  const target = new Date(`${value}T00:00:00`)
  return Math.max(0, Math.ceil((target.getTime() - today.getTime()) / 86_400_000))
}

function PlanCard({
  title,
  step,
  children,
}: {
  title: string
  step: number
  children: ReactNode
}) {
  return (
    <section className="rounded-[20px] bg-white px-5 py-5">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-[18px] font-bold tracking-[-0.03em]">{title}</h2>
        <Link to={`/plan/${step}?edit=1`} className="flex min-h-8 items-center gap-0.5 text-sm font-bold text-[var(--brand)]">
          수정 <ChevronRight aria-hidden="true" className="size-4" strokeWidth={2.5} />
        </Link>
      </div>
      <dl className="space-y-3.5">{children}</dl>
    </section>
  )
}

function PlanRow({ label, value, accent = false }: { label: string; value: string; accent?: boolean }) {
  return (
    <div className="flex items-center justify-between gap-4 text-[15px]">
      <dt className="text-[var(--text-secondary)]">{label}</dt>
      <dd className={accent ? "text-[15px] font-bold text-[#ff735d]" : "text-[15px] font-semibold tabular-nums"}>{value}</dd>
    </div>
  )
}

function MySettlementPlanPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const showBackButton = searchParams.get("from") === "home"
  const { data: plan, isError: error } = useQuery<SettlementPlan>({
    queryKey: ["settlement-plan"],
    queryFn: () => fetchSettlementPlan(),
    staleTime: 30_000,
  })

  return (
    <main className="min-h-dvh bg-[#f5f6f7] pb-[calc(120px+env(safe-area-inset-bottom))]">
      <header className="bg-white pt-[env(safe-area-inset-top)]">
        <div className={showBackButton ? "grid h-14 grid-cols-[44px_1fr_44px] items-center px-2" : "grid h-14 grid-cols-1 items-center px-2"}>
          {showBackButton && <button type="button" onClick={() => void navigate("/home")} className="grid size-11 place-items-center rounded-full" aria-label="홈으로 돌아가기">
            <ArrowLeft aria-hidden="true" className="size-5" />
          </button>}
          <h1 className="text-center text-sm font-bold">마이</h1>
        </div>
      </header>

      <section className="px-4 pt-6">
        <h2 className="text-[28px] font-bold tracking-[-0.05em]">나의 정착 계획</h2>
        <p className="mt-2 text-[15px] text-[var(--text-secondary)]">계산에 사용되는 정보를 확인하고 수정할 수 있어요.</p>

        {error && <p className="mt-8 rounded-2xl bg-white p-5 text-sm text-[#e0523d]">정착 계획을 불러오지 못했어요. 잠시 후 다시 시도해주세요.</p>}
        {!plan && !error && <div className="flex justify-center py-20 text-[var(--brand)]"><LoaderCircle className="size-7 animate-spin" aria-label="불러오는 중" /></div>}
        {plan && (
          <div className="mt-6 space-y-4">
            <PlanCard title="체류 계획" step={1}>
              <PlanRow label="입주 예정일" value={formatDate(plan.moveInDate)} />
              <PlanRow label="입주일까지" value={`D-${daysUntil(plan.moveInDate)}`} />
              <PlanRow label="예상 체류기간" value={`${plan.stayMonths}개월`} />
            </PlanCard>
            <PlanCard title="보유 자금" step={3}>
              <PlanRow label="원화 준비자금" value={formatKrw(plan.availableKrw)} />
              <PlanRow label="보유 엔화" value={formatJpy(plan.availableJpy)} />
              <PlanRow label="비상예비비" value={formatKrw(plan.emergencyKrw)} />
            </PlanCard>
            <PlanCard title="개인 추가 초기비용" step={5}>
              <PlanRow label="합계" value={`${formatKrw(plan.additionalKrw)} · ${formatJpy(plan.additionalJpy)}`} />
              <PlanRow label="입력 상태" value="직접 입력" />
            </PlanCard>
            <PlanCard title="월 생활비" step={6}>
              <PlanRow label="월 합계" value={formatJpy(plan.monthlyJpy)} />
              <PlanRow label="입력 방식" value="기본값 적용 · 아직 확인 전" accent />
            </PlanCard>
            <section className="rounded-[20px] bg-white px-5 py-5">
              <div className="mb-4 flex items-center justify-between">
                <h2 className="text-[18px] font-bold tracking-[-0.03em]">적용 환율</h2>
                <Link to="/plan/exchange-rate" className="flex min-h-8 items-center gap-0.5 text-sm font-bold text-[var(--brand)]">
                  보기 <ChevronRight aria-hidden="true" className="size-4" strokeWidth={2.5} />
                </Link>
              </div>
              <dl className="space-y-3.5">
              <PlanRow label="기준 환율" value={`¥100 = ₩${plan.exchangeRate}`} />
              <PlanRow label="업데이트" value={formatDate(plan.exchangeRateUpdatedAt)} />
              </dl>
            </section>
          </div>
        )}
      </section>
      <BottomNav />
    </main>
  )
}

export { MySettlementPlanPage }
