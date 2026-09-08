import { useState } from "react"
import { ArrowLeft, Check } from "lucide-react"
import { useNavigate } from "react-router-dom"

import { Button } from "@/components/ui/button"
import { JPY_TO_KRW_EXCHANGE_RATE } from "@/constants/currency"
import { updateSettlementPlan } from "@/features/settlement-plan/api/settlement-plan-api"

function ExchangeRateEditPage() {
  const navigate = useNavigate()
  const [rate, setRate] = useState(String(JPY_TO_KRW_EXCHANGE_RATE))
  const [isSaving, setIsSaving] = useState(false)

  const handleSave = async () => {
    const parsedRate = Number(rate.replaceAll(",", ""))
    if (!Number.isFinite(parsedRate) || parsedRate <= 0) return

    setIsSaving(true)
    await updateSettlementPlan({ exchangeRate: parsedRate, exchangeRateUpdatedAt: "2026-09-08" })
    void navigate("/my")
  }

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
        <h2 className="text-[19px] font-bold tracking-[-0.03em]">계산에 적용할 환율을 입력해주세요</h2>
        <p className="mt-1 text-xs leading-5 text-[var(--text-secondary)]">100엔을 원화로 환산할 기준을 설정해요.</p>

        <div className="mt-6 rounded-xl bg-white p-5 shadow-[0_1px_2px_rgb(15_23_42/0.04)]">
          <label className="block text-xs font-bold text-[var(--brand)]" htmlFor="exchange-rate">기준 환율</label>
          <div className="mt-3 flex items-end gap-3 border-b-2 border-[var(--brand)] pb-3">
            <span className="text-[22px] font-bold">¥100 = ₩</span>
            <input
              id="exchange-rate"
              type="text"
              inputMode="numeric"
              value={rate ? Number(rate).toLocaleString("ko-KR") : ""}
              onChange={(event) => setRate(event.target.value.replace(/[^0-9]/g, ""))}
              className="min-w-0 flex-1 bg-transparent text-right text-[22px] font-bold tabular-nums outline-none"
              aria-label="100엔 환율"
            />
          </div>
          <p className="mt-4 flex items-center gap-1 text-xs text-[var(--text-secondary)]"><Check className="size-4 text-[var(--brand)]" />2026.09.08 기준으로 적용돼요.</p>
        </div>
      </section>
      <div className="fixed inset-x-0 bottom-0 z-20 mx-auto w-full max-w-[430px] bg-gradient-to-t from-[#f5f6f7] from-70% to-transparent px-4 pt-6 pb-[calc(14px+env(safe-area-inset-bottom))]">
        <Button type="button" onClick={() => void handleSave()} disabled={isSaving || !rate} className="h-12 w-full rounded-lg bg-[var(--brand)] text-[15px] font-bold text-white hover:bg-[var(--brand)]/90">
          {isSaving ? "저장 중..." : "저장하기"}
        </Button>
      </div>
    </main>
  )
}

export { ExchangeRateEditPage }
