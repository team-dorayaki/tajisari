import { Link } from "react-router-dom"
import { useQuery } from "@tanstack/react-query"

import avatarUrl from "@/assets/default_avatar.png"
import { fetchSettlementPlan, type SettlementPlan } from "@/features/settlement-plan/api/settlement-plan-api"

function daysUntil(moveInDate: string) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const target = new Date(`${moveInDate}T00:00:00`)
  return Math.max(0, Math.ceil((target.getTime() - today.getTime()) / 86_400_000))
}

function formatPreparedFunds(value: number) {
  return value % 10_000 === 0 ? `${(value / 10_000).toLocaleString("ko-KR")}만원` : `₩${value.toLocaleString("ko-KR")}`
}

function SettlementProfileCard() {
  const { data: plan } = useQuery<SettlementPlan>({
    queryKey: ["settlement-plan"],
    queryFn: fetchSettlementPlan,
    staleTime: 30_000,
  })

  if (!plan) return null

  const profileValues = [
    { label: "입주 예정", value: `D-${daysUntil(plan.moveInDate)}` },
    { label: "체류 기간", value: `${plan.stayMonths}개월` },
    { label: "준비자금", value: formatPreparedFunds(plan.availableKrw) },
    { label: "월 생활비", value: `¥${plan.monthlyJpy.toLocaleString("ko-KR")}`, accent: true },
  ]

  return (
    <section className="rounded-[20px] bg-white p-5" aria-labelledby="settlement-profile-title">
      <div className="flex items-center gap-4 border-b border-[#edf0f2] pb-4">
        <img src={avatarUrl} alt="" aria-hidden="true" className="size-14 shrink-0 rounded-full object-cover" />
        <div className="min-w-0 flex-1">
          <h2 id="settlement-profile-title" className="text-base font-bold">
            나의 정착 프로필
          </h2>
          <p className="mt-1 text-xs text-[var(--text-secondary)]">계획 기준 설정 완료</p>
        </div>
        <Link to="/my?from=home" className="text-xs font-semibold text-[var(--brand)]">
          수정하기 ›
        </Link>
      </div>

      <dl className="mt-4 grid grid-cols-2 gap-x-7 gap-y-4">
        {profileValues.map(({ label, value, accent }) => (
          <div key={label} className="flex items-center justify-between gap-2">
            <dt className="text-xs text-[var(--text-secondary)]">{label}</dt>
            <dd className={accent ? "text-sm font-bold tabular-nums text-[var(--brand)]" : "text-sm font-bold tabular-nums"}>
              {value}
            </dd>
          </div>
        ))}
      </dl>
    </section>
  )
}

export { SettlementProfileCard }
