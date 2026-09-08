import { useEffect, useMemo, useState } from "react"
import { ArrowDown, ArrowLeft, ArrowUp, CheckCircle2, LoaderCircle, X } from "lucide-react"
import { useNavigate, useSearchParams } from "react-router-dom"

import { BottomNav } from "@/components/layout/bottom-nav"
import { fetchProperties, fetchPropertyComparisons } from "@/features/properties/api/properties-api"
import type { PropertyComparison, PropertySummary } from "@/features/properties/api/properties-api"
import { cn } from "@/lib/utils"

type ComparisonItem = Omit<PropertySummary, "livingMonths"> & PropertyComparison
type LoadState = "loading" | "success" | "error"

const columnColors = ["#315c58", "#00ad96", "#99a3ad"]

function formatYen(value: number) {
  return `¥${value.toLocaleString("en-US")}`
}

function formatMan(value: number) {
  return `¥${(value / 10_000).toFixed(value % 10_000 === 0 ? 0 : 1)}만`
}

function ComparisonBars({ items, field, formatter }: { items: ComparisonItem[]; field: "livingMonths" | "initialSettlementCost"; formatter: (value: number) => string }) {
  const maximum = Math.max(...items.map((item) => item[field]))
  return (
    <div className="flex h-32 items-end justify-around gap-3 border-b border-[#dce3e6] px-2 pt-5">
      {items.map((item, index) => (
        <div key={item.id} className="flex h-full min-w-0 flex-1 flex-col items-center justify-end gap-1.5">
          <span className="text-[11px] font-bold">{formatter(item[field])}</span>
          <span className="w-6 rounded-t-md" style={{ height: `${Math.max(20, (item[field] / maximum) * 72)}px`, backgroundColor: columnColors[index] }} />
          <span className="w-full truncate text-center text-[11px] text-[var(--text-secondary)]">{String.fromCharCode(65 + index)}</span>
        </div>
      ))}
    </div>
  )
}

function PropertyComparisonPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [state, setState] = useState<LoadState>("loading")
  const [items, setItems] = useState<ComparisonItem[]>([])
  const [activeTab, setActiveTab] = useState<"funds" | "notes">("funds")
  const [prioritySheetOpen, setPrioritySheetOpen] = useState(false)
  const [priorityIds, setPriorityIds] = useState<string[]>([])
  const selectedIds = useMemo(() => [...new Set((searchParams.get("ids") ?? "").split(",").filter(Boolean))].slice(0, 3), [searchParams])
  const hasValidSelection = selectedIds.length >= 2
  const displayState = hasValidSelection ? state : "error"

  useEffect(() => {
    let active = true
    if (!hasValidSelection) return () => { active = false }

    Promise.all([fetchProperties(), fetchPropertyComparisons(selectedIds)])
      .then(([properties, comparisons]) => {
        if (!active) return
        const propertyById = new Map(properties.map((property) => [property.id, property]))
        const comparisonById = new Map(comparisons.map((comparison) => [comparison.propertyId, comparison]))
        const comparisonItems = selectedIds.flatMap((id) => {
          const property = propertyById.get(id)
          const comparison = comparisonById.get(id)
          return property && comparison ? [{ ...property, ...comparison }] : []
        })
        if (comparisonItems.length < 2) {
          setState("error")
          return
        }
        setItems(comparisonItems)
        setState("success")
      })
      .catch(() => { if (active) setState("error") })

    return () => { active = false }
  }, [hasValidSelection, selectedIds])

  const lowestInitialCost = items.reduce<ComparisonItem | null>((lowest, item) => !lowest || item.initialSettlementCost < lowest.initialSettlementCost ? item : lowest, null)
  const comparisonGridStyle = { gridTemplateColumns: `96px repeat(${items.length}, minmax(0, 1fr))` }

  function openPrioritySheet() {
    setPriorityIds(items.map((item) => item.id))
    setPrioritySheetOpen(true)
  }

  function movePriority(id: string, direction: -1 | 1) {
    setPriorityIds((current) => {
      const index = current.indexOf(id)
      const nextIndex = index + direction
      if (index < 0 || nextIndex < 0 || nextIndex >= current.length) return current
      const next = [...current]
      ;[next[index], next[nextIndex]] = [next[nextIndex], next[index]]
      return next
    })
  }

  return (
    <main className="min-h-dvh bg-[#f5f6f7] pb-[calc(164px+env(safe-area-inset-bottom))]">
      <header className="grid h-[calc(72px+env(safe-area-inset-top))] grid-cols-[64px_1fr_64px] items-center bg-white px-2 pt-[env(safe-area-inset-top)]">
        <button type="button" onClick={() => void navigate("/properties?tab=compare")} className="grid min-h-11 place-items-center" aria-label="비교 매물 선택으로 돌아가기"><ArrowLeft className="size-5" /></button>
        <h1 className="text-center text-base font-bold">매물 비교</h1>
        <span />
      </header>

      <div className="border-b border-[#edf0f2] bg-white px-4">
        <div className="grid grid-cols-2 text-sm font-bold">
          <button type="button" onClick={() => setActiveTab("funds")} className={cn("min-h-12 border-b-2", activeTab === "funds" ? "border-[var(--brand)] text-[var(--brand)]" : "border-transparent text-[var(--text-secondary)]")}>자금 비교</button>
          <button type="button" onClick={() => setActiveTab("notes")} className={cn("min-h-12 border-b-2", activeTab === "notes" ? "border-[var(--brand)] text-[var(--brand)]" : "border-transparent text-[var(--text-secondary)]")}>조건 · 주의사항</button>
        </div>
      </div>

      {displayState === "loading" && <div className="flex min-h-80 flex-col items-center justify-center gap-3"><LoaderCircle className="size-7 animate-spin text-[var(--brand)]" /><p className="text-sm font-semibold text-[var(--text-secondary)]">비교 결과를 불러오고 있어요</p></div>}
      {displayState === "error" && <div className="flex min-h-80 flex-col items-center justify-center px-6 text-center"><p className="text-base font-bold">비교할 매물을 2개 이상 선택해주세요</p><button type="button" onClick={() => void navigate("/properties?tab=compare")} className="mt-4 h-11 rounded-lg bg-[var(--brand)] px-5 text-sm font-bold text-white">매물 선택하기</button></div>}

      {displayState === "success" && activeTab === "funds" && (
        <div className="space-y-3 py-4">
          <section className="bg-white px-4 py-5">
            <h2 className="text-base font-bold">비교 요약</h2>
            <div className="mt-1 grid grid-cols-2 divide-x divide-[#edf0f2]">
              <div className="px-2 py-2"><ComparisonBars items={items} field="livingMonths" formatter={(value) => value.toFixed(1)} /><p className="pt-2 text-center text-[11px] text-[var(--text-secondary)]">생활 가능기간 비교</p></div>
              <div className="px-2 py-2"><ComparisonBars items={items} field="initialSettlementCost" formatter={(value) => `${Math.round(value / 1000)}K`} /><p className="pt-2 text-center text-[11px] text-[var(--text-secondary)]">초기 정착비 비교</p></div>
            </div>
          </section>

          {lowestInitialCost && <section className="mx-4 rounded-2xl bg-[var(--brand-soft)] px-4 py-3.5"><div className="flex items-start gap-2"><CheckCircle2 aria-hidden="true" className="mt-0.5 size-5 shrink-0 text-[var(--brand)]" /><div><h2 className="text-sm font-bold">비용 부담은 {lowestInitialCost.name.replace(" 스튜디오", "").replace(" 원룸", "")}가 가장 낮아요</h2><p className="mt-1 text-[11px] leading-4 text-[#47736d]">생활 가능기간은 가장 길고, 초기 정착비는 가장 적어요.</p></div></div></section>}

          <section className="bg-white px-4 py-5">
            <h2 className="text-base font-bold">핵심 비교</h2>
            <div className="mt-3">
              <div className="grid text-center text-[11px] font-bold" style={comparisonGridStyle}>
                <span />
                {items.map((item, index) => <span key={item.id} className={cn("min-w-0 px-1 py-1.5", index === 1 && "rounded-t-lg bg-[#f2f7f7]")} style={{ color: columnColors[index] }}><img src={item.images[0]?.src} alt="" className="mx-auto mb-1 size-12 rounded-xl object-cover" />{item.name.replace(" 원룸", "").replace(" 스튜디오", "")}</span>)}
              </div>
              {([
                ["월 주거비", (item: ComparisonItem) => formatYen(item.monthlyHousingCost)],
                ["초기 정산", (item: ComparisonItem) => formatYen(item.initialSettlementCost)],
                ["입주 후 잔액", (item: ComparisonItem) => formatMan(item.balanceAfterMoveIn)],
                ["생활 가능", (item: ComparisonItem) => `${item.livingMonths.toFixed(1)}개월`],
              ] as Array<[string, (item: ComparisonItem) => string]>).map(([label, formatter]) => <div key={label} className="grid text-center text-xs" style={comparisonGridStyle}><span className="px-2 py-3 text-left font-medium text-[var(--text-secondary)]">{label}</span>{items.map((item, index) => <span key={item.id} className={cn("px-1 py-3 font-bold", index === 1 && "bg-[#f2f7f7] text-[var(--brand)]")}>{formatter(item)}</span>)}</div>)}
            </div>
          </section>

          <section className="bg-white px-4 py-5"><h2 className="text-base font-bold">초기비용 구성</h2><div className="mt-3">{[["비반환 비용", "nonRefundableCost"], ["반환 가능 보증금", "refundableDeposit"]].map(([label, field]) => <div key={label} className="grid text-center text-xs" style={comparisonGridStyle}><span className="px-2 py-2 text-left font-medium text-[var(--text-secondary)]">{label}</span>{items.map((item, index) => <span key={item.id} className={cn("px-1 py-2 font-bold", index === 1 && "text-[var(--brand)]")}>{formatYen(item[field as "refundableDeposit" | "nonRefundableCost"]).replace(",000", "K")}</span>)}</div>)}</div><p className="mt-4 text-[11px] leading-5 text-[var(--text-secondary)]">계약 시 필요한 현금을 반환 여부로 나눠 비교해요.</p></section>
        </div>
      )}

      {displayState === "success" && activeTab === "notes" && <div className="space-y-3 py-4">{items.map((item, index) => <section key={item.id} className="bg-white px-4 py-5"><h2 className="text-sm font-bold" style={{ color: columnColors[index] }}>{item.name}</h2><ul className="mt-3 space-y-2 text-sm leading-5 text-[var(--text-secondary)]">{item.notes.map((note) => <li key={note} className="flex gap-2"><span className="mt-2 size-1.5 shrink-0 rounded-full bg-[#98a1aa]" />{note}</li>)}</ul></section>)}</div>}

      {displayState === "success" && <div className="fixed inset-x-0 bottom-[calc(100px+env(safe-area-inset-bottom))] z-20 mx-auto w-full max-w-[430px] px-4"><button type="button" onClick={openPrioritySheet} className="flex h-12 w-full items-center justify-center rounded-lg bg-[var(--brand)] text-sm font-bold text-white shadow-sm">매물 우선순위 정하기</button></div>}
      <BottomNav />
      {prioritySheetOpen && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/30" role="presentation" onMouseDown={() => setPrioritySheetOpen(false)}>
          <section role="dialog" aria-modal="true" aria-labelledby="priority-sheet-title" className="w-full max-w-[430px] rounded-t-3xl bg-white px-5 pb-[calc(20px+env(safe-area-inset-bottom))] pt-3 shadow-2xl" onMouseDown={(event) => event.stopPropagation()}>
            <div className="mx-auto h-1 w-10 rounded-full bg-[#dde2e5]" />
            <div className="mt-5 flex items-start justify-between"><div><h2 id="priority-sheet-title" className="text-lg font-bold">매물 우선순위 정하기</h2><p className="mt-1 text-xs text-[var(--text-secondary)]">마음에 드는 순서대로 정렬해주세요.</p></div><button type="button" onClick={() => setPrioritySheetOpen(false)} className="grid size-10 place-items-center text-[var(--text-secondary)]" aria-label="닫기"><X className="size-5" /></button></div>
            <ol className="mt-5 divide-y divide-[#edf0f2] rounded-xl border border-[#e7ebed]">
              {priorityIds.map((id, index) => { const item = items.find((candidate) => candidate.id === id); if (!item) return null; return <li key={id} className="flex items-center gap-3 px-3 py-3"><span className={cn("grid size-7 shrink-0 place-items-center rounded-full text-sm font-bold", index === 0 ? "bg-[var(--brand)] text-white" : "bg-[#eef1f2] text-[var(--text-secondary)]")}>{index + 1}</span><img src={item.images[0]?.src} alt="" className="size-12 rounded-lg object-cover" /><span className="min-w-0 flex-1 truncate text-sm font-bold">{item.name}</span><div className="flex items-center gap-0.5"><button type="button" disabled={index === 0} onClick={() => movePriority(id, -1)} className="grid size-9 place-items-center rounded-lg text-[var(--text-secondary)] disabled:opacity-25" aria-label="위로 이동"><ArrowUp className="size-4" /></button><button type="button" disabled={index === priorityIds.length - 1} onClick={() => movePriority(id, 1)} className="grid size-9 place-items-center rounded-lg text-[var(--text-secondary)] disabled:opacity-25" aria-label="아래로 이동"><ArrowDown className="size-4" /></button></div></li> })}
            </ol>
            <button type="button" onClick={() => setPrioritySheetOpen(false)} className="mt-5 flex h-12 w-full items-center justify-center rounded-lg bg-[var(--brand)] text-sm font-bold text-white">이 순서로 저장하기</button>
          </section>
        </div>
      )}
    </main>
  )
}

export { PropertyComparisonPage }
