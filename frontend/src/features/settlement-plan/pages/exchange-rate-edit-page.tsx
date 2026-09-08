import { ArrowLeft, Check } from "lucide-react"
import { useNavigate } from "react-router-dom"

import { Button } from "@/components/ui/button"
import { JPY_TO_KRW_EXCHANGE_RATE } from "@/constants/currency"

function ExchangeRateEditPage() {
  const navigate = useNavigate()
  const today = new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Seoul" }).format(new Date())

  return (
    <main className="flex min-h-dvh flex-col bg-[#f5f6f7] pb-[calc(92px+env(safe-area-inset-bottom))]">
      <header className="bg-white pt-[env(safe-area-inset-top)]">
        <div className="grid h-14 grid-cols-[44px_1fr_44px] items-center px-2">
          <button type="button" onClick={() => void navigate("/my")} className="grid size-11 place-items-center rounded-full" aria-label="마이로 돌아가기">
            <ArrowLeft aria-hidden="true" className="size-5" />
          </button>
          <h1 className="text-center text-sm font-bold">적용 환율 수정</h1>
        </div>
      </header>
      <section className="flex-1 px-4 pt-8">
        <h2 className="text-[19px] font-bold tracking-[-0.03em]">계산에 적용하는 환율이에요</h2>
        <p className="mt-1 text-xs leading-5 text-[var(--text-secondary)]">환율은 100엔당 860원으로 고정 적용돼요.</p>

        <div className="mt-6 rounded-xl bg-white p-5 shadow-[0_1px_2px_rgb(15_23_42/0.04)]">
          <p className="block text-xs font-bold text-[var(--brand)]">기준 환율</p>
          <div className="mt-3 flex items-end gap-3 border-b-2 border-[var(--brand)] pb-3">
            <span className="text-[22px] font-bold">¥100 = ₩</span>
            <strong className="min-w-0 flex-1 text-right text-[22px] font-bold tabular-nums">{JPY_TO_KRW_EXCHANGE_RATE.toLocaleString("ko-KR")}</strong>
          </div>
          <p className="mt-4 flex items-center gap-1 text-xs text-[var(--text-secondary)]"><Check className="size-4 text-[var(--brand)]" />{today.replaceAll("-", ".")} 기준으로 적용돼요.</p>
        </div>
      </section>
      <div className="fixed inset-x-0 bottom-0 z-20 mx-auto w-full max-w-[430px] bg-gradient-to-t from-[#f5f6f7] from-70% to-transparent px-4 pt-6 pb-[calc(14px+env(safe-area-inset-bottom))]">
        <Button type="button" onClick={() => void navigate("/my")} className="h-12 w-full rounded-lg bg-[var(--brand)] text-[15px] font-bold text-white hover:bg-[var(--brand)]/90">
          확인
        </Button>
      </div>
    </main>
  )
}

export { ExchangeRateEditPage }
