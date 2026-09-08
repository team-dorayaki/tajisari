import { CircleAlert, Coins, JapaneseYen, WalletCards } from "lucide-react"

import logoUrl from "@/assets/logo.svg"

type PropertyFinancialSimulationProps = {
  availableKrw: number
  exchangeRate: number
  initialCost: number
  monthlyHousingCost: number
  monthlyLivingCost: number
  estimatedExitCost: number
  stayMonths: number
  excludedCosts: Array<{ label: string; category: string }>
}

function formatYen(amount: number) {
  return `¥${Math.round(amount).toLocaleString("en-US")}`
}

function formatKrw(amount: number) {
  return `₩${Math.round(amount).toLocaleString("ko-KR")}`
}

function getSimulationData({ availableKrw, exchangeRate, initialCost, monthlyHousingCost, monthlyLivingCost, stayMonths }: Omit<PropertyFinancialSimulationProps, "excludedCosts" | "estimatedExitCost">) {
  const availableFunds = Math.round(availableKrw / exchangeRate)
  const laterMonthlyCost = monthlyHousingCost + monthlyLivingCost
  const balanceAfterMoveIn = availableFunds - initialCost
  const usableMonths = Math.max(0, Math.round((balanceAfterMoveIn / Math.max(laterMonthlyCost, 1)) * 10) / 10)
  const targetBalance = balanceAfterMoveIn - stayMonths * laterMonthlyCost

  return { availableFunds, laterMonthlyCost, balanceAfterMoveIn, usableMonths, targetBalance }
}

function BalanceChart({ availableFunds, initialCost, monthlyCost, usableMonths }: { availableFunds: number; initialCost: number; monthlyCost: number; usableMonths: number }) {
  const chartWidth = 320
  const chartHeight = 150
  const left = 22
  const right = 304
  const top = 18
  const bottom = 120
  const maxBalance = Math.max(availableFunds, initialCost, monthlyCost * 3, 1)
  const pointCount = Math.min(Math.max(Math.ceil(usableMonths) + 2, 5), 7)
  const values = Array.from({ length: pointCount }, (_, index) => availableFunds - initialCost - index * monthlyCost)
  const minValue = Math.min(...values, 0)
  const maxValue = Math.max(...values, maxBalance)
  const valueRange = Math.max(maxValue - minValue, 1)
  const xAt = (index: number) => left + (index / Math.max(pointCount - 1, 1)) * (right - left)
  const yAt = (value: number) => top + ((maxValue - value) / valueRange) * (bottom - top)
  const zeroY = yAt(0)
  const shortageIndex = values.findIndex((value) => value < 0)
  const depletionX = shortageIndex > 0
    ? xAt(shortageIndex - 1) + (values[shortageIndex - 1] / (values[shortageIndex - 1] - values[shortageIndex])) * (xAt(shortageIndex) - xAt(shortageIndex - 1))
    : shortageIndex === 0 ? left : null

  return (
    <div className="mt-5" aria-label="월별 예상 잔액 그래프">
      <svg viewBox={`0 0 ${chartWidth} ${chartHeight}`} className="h-auto w-full overflow-visible" role="img" aria-label="입주 후 월별 예상 잔액">
        {[0, 0.5, 1].map((ratio) => {
          const y = top + (bottom - top) * ratio
          return <line key={ratio} x1={left} x2={right} y1={y} y2={y} stroke="#e6eaed" strokeWidth="1" />
        })}
        <line x1={left} x2={right} y1={zeroY} y2={zeroY} stroke="#c8d0d6" strokeWidth="1" />
        {values.slice(1).map((endValue, index) => {
          const startValue = values[index]
          const startX = xAt(index)
          const endX = xAt(index + 1)

          if (startValue >= 0 && endValue < 0) {
            const crossingX = startX + (startValue / (startValue - endValue)) * (endX - startX)
            return <g key={index}><line x1={startX} y1={yAt(startValue)} x2={crossingX} y2={zeroY} stroke="var(--brand)" strokeWidth="3" strokeLinecap="round" /><line x1={crossingX} y1={zeroY} x2={endX} y2={yAt(endValue)} stroke="#ff705d" strokeWidth="3" strokeLinecap="round" /></g>
          }

          return <line key={index} x1={startX} y1={yAt(startValue)} x2={endX} y2={yAt(endValue)} stroke={endValue < 0 ? "#ff705d" : "var(--brand)"} strokeWidth="3" strokeLinecap="round" />
        })}
        {values.map((value, index) => <circle key={index} cx={xAt(index)} cy={yAt(value)} r="4.5" fill={value < 0 ? "#ff705d" : "var(--brand)"} />)}
        {depletionX !== null && <line x1={depletionX} x2={depletionX} y1={top} y2={bottom} stroke="#ff705d" strokeWidth="1.5" />}
        <text x="0" y={yAt(maxValue) + 4} fill="#9aa3ac" fontSize="10">¥{Math.round(maxValue / 1000)}K</text>
        <text x="0" y={zeroY + 4} fill="#9aa3ac" fontSize="10">¥0</text>
        {depletionX !== null && <text x={Math.min(Math.max(depletionX - 18, left), right - 35)} y={top - 5} fill="#ff705d" fontSize="10">자금 소진</text>}
      </svg>
      <div className="mt-1 flex justify-between pl-5 pr-2 text-[10px] text-[var(--text-secondary)]">
        {Array.from({ length: pointCount }, (_, index) => <span key={index} className={index === shortageIndex ? "font-bold text-[#ff705d]" : ""}>{index === 0 ? "입주" : `${index}개월`}</span>)}
      </div>
    </div>
  )
}

function SimulationRow({ icon: Icon, label, value, tone = "default" }: { icon: typeof Coins; label: string; value: string; tone?: "default" | "brand" }) {
  return <div className="flex items-center gap-3 py-2.5"><span className={`grid size-8 shrink-0 place-items-center rounded-full ${tone === "brand" ? "bg-[var(--brand-soft)] text-[var(--brand)]" : "bg-[#f1f3f4] text-[#707b86]"}`}><Icon aria-hidden="true" className="size-4" /></span><span className="flex-1 text-sm text-[var(--text-secondary)]">{label}</span><strong className={tone === "brand" ? "text-sm tabular-nums text-[var(--brand)]" : "text-sm tabular-nums"}>{value}</strong></div>
}

function PropertyFinancialSimulation({ availableKrw, exchangeRate, initialCost, monthlyHousingCost, monthlyLivingCost, estimatedExitCost, stayMonths, excludedCosts }: PropertyFinancialSimulationProps) {
  const simulation = getSimulationData({ availableKrw, exchangeRate, initialCost, monthlyHousingCost, monthlyLivingCost, stayMonths })
  const canCoverTarget = simulation.targetBalance >= 0
  const shortageYen = Math.abs(simulation.targetBalance)
  const shortageKrw = shortageYen * exchangeRate
  const timingCosts = [initialCost, simulation.laterMonthlyCost, estimatedExitCost]
  const largestTimingCost = Math.max(...timingCosts, 1)
  const timingBarHeights = timingCosts.map((cost) => `${Math.max((cost / largestTimingCost) * 112, 20)}px`)

  return (
    <div className="bg-[#f5f6f7]">
      <section className="bg-white px-4 pt-7 pb-5">
        <div className="flex items-center justify-between"><p className="text-xs text-[var(--text-secondary)]">적용 환율</p><span className="rounded-full bg-[var(--brand-soft)] px-2.5 py-1 text-[10px] font-bold text-[var(--brand)]">1 JPY = ₩{exchangeRate.toFixed(1)}</span></div>
        <div className="mt-6"><p className="text-sm text-[var(--text-secondary)]">현재 자금으로</p><h2 className="mt-1 text-xl font-bold tracking-[-0.03em]">약 <span className="text-[var(--brand)]">{simulation.usableMonths}개월</span> 동안<br />무소득으로 생활할 수 있어요</h2></div>
        <div className="mt-6 flex items-center justify-between text-[11px]"><span className="font-bold text-[var(--brand)]">생활 가능 기간</span><span className="text-[var(--text-secondary)]">예상 체류기간 {stayMonths}개월</span></div>
        <div className="mt-2 h-2 overflow-hidden rounded-full bg-[#dce2e5]"><span className="block h-full rounded-full bg-[var(--brand)]" style={{ width: `${Math.min((simulation.usableMonths / Math.max(stayMonths, 1)) * 100, 100)}%` }} /></div>
        <dl className="mt-5 divide-y divide-[#eef0f2] border-y border-[#eef0f2]"><SimulationRow icon={Coins} label="사용 가능 자금" value={formatYen(simulation.availableFunds)} /><SimulationRow icon={JapaneseYen} label="예상 초기 정착비" value={`− ${formatYen(initialCost)}`} /><SimulationRow icon={WalletCards} label="입주 후 잔액" value={formatYen(simulation.balanceAfterMoveIn)} tone="brand" /></dl>
        <div className="mt-5 rounded-2xl bg-[#def7f2] p-4"><div className="flex items-center gap-2"><img src={logoUrl} alt="" aria-hidden="true" className="size-8 object-contain" /><strong className="text-sm">생활가능기간 계산은 이렇게 했어요</strong></div><dl className="mt-3 space-y-2 text-[11px]"><div className="flex justify-between"><dt className="text-[var(--brand)]">포함한 비용</dt><dd>확정 금액 · 선택한 비용</dd></div><div className="flex justify-between"><dt className="text-[#ff705d]">제외한 비용</dt><dd>{excludedCosts.length > 0 ? `확인 필요 비용 ${excludedCosts.length}개` : "없음"}</dd></div><div className="flex justify-between"><dt className="text-[var(--text-secondary)]">참고로 안내</dt><dd>갱신 · 퇴거 · 조건부 비용</dd></div></dl></div>
      </section>

      <section className="mt-2 bg-white px-4 py-6"><h2 className="text-lg font-bold">시점별 예상 비용</h2><p className="mt-1 text-xs text-[var(--text-secondary)]">입주할 때, 매월, 퇴거할 때의 비용을 나눠봤어요.</p><div className="mt-5 grid grid-cols-3 items-end gap-3"><div className="text-center"><p className="text-xs font-bold text-[var(--brand)]">{formatYen(initialCost)}</p><div className="mx-auto mt-2 w-12 rounded-t-lg bg-[var(--brand)]" style={{ height: timingBarHeights[0] }} /><p className="mt-3 text-xs font-bold">입주할 때</p><p className="mt-1 text-[10px] text-[var(--text-secondary)]">초기 정착비</p></div><div className="text-center"><p className="text-xs font-bold text-[#9aa3ac]">{formatYen(simulation.laterMonthlyCost)}</p><div className="mx-auto mt-2 w-12 rounded-t-lg bg-[#aeb8c4]" style={{ height: timingBarHeights[1] }} /><p className="mt-3 text-xs font-bold">매월</p><p className="mt-1 text-[10px] text-[var(--text-secondary)]">주거비 + 생활비</p></div><div className="text-center"><p className="text-xs font-bold text-[#ff705d]">약 {formatYen(estimatedExitCost)}</p><div className="mx-auto mt-2 w-12 rounded-t-lg bg-[#ff705d]" style={{ height: timingBarHeights[2] }} /><p className="mt-3 text-xs font-bold">퇴거할 때</p><p className="mt-1 text-[10px] text-[var(--text-secondary)]">참고 예상액</p></div></div></section>

      <section className="mt-2 bg-white px-4 py-6"><h2 className="text-lg font-bold">월별 예상 잔액</h2><p className="mt-1 text-xs text-[var(--text-secondary)]">첫 달 생활비와 이후 월 반복지출을 반영한 예상이에요.</p><BalanceChart availableFunds={simulation.availableFunds} initialCost={initialCost} monthlyCost={simulation.laterMonthlyCost} usableMonths={simulation.usableMonths} /><div className={`mt-5 flex items-center gap-2 rounded-lg px-3 py-3 text-xs font-semibold ${canCoverTarget ? "bg-[var(--brand-soft)] text-[var(--brand)]" : "bg-[#fff0ed] text-[#ff705d]"}`}><CircleAlert aria-hidden="true" className="size-4 shrink-0" /><span>{canCoverTarget ? `${stayMonths}개월 체류 후 ${formatYen(simulation.targetBalance)}가 남아요.` : `${stayMonths}개월 체류하려면 ${formatYen(shortageYen)} (약 ${formatKrw(shortageKrw)})를 추가로 준비해야 해요.`}</span></div></section>
    </div>
  )
}

export { PropertyFinancialSimulation }
