import { UserRound } from "lucide-react"
import { Link } from "react-router-dom"

const profileValues = [
  { label: "입주 예정", value: "D-42" },
  { label: "체류 기간", value: "12개월" },
  { label: "준비자금", value: "800만원" },
  { label: "월 생활비", value: "¥115,000", accent: true },
]

function SettlementProfileCard() {
  return (
    <section className="rounded-[20px] bg-white p-5" aria-labelledby="settlement-profile-title">
      <div className="flex items-center gap-4 border-b border-[#edf0f2] pb-4">
        <div className="grid size-14 shrink-0 place-items-center rounded-full bg-[var(--brand-soft)] text-[var(--brand)]">
          <UserRound aria-hidden="true" className="size-9" strokeWidth={1.8} />
        </div>
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
