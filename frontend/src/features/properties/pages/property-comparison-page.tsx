import { useEffect, useMemo, useState } from "react"
import { useQueryClient } from "@tanstack/react-query"
import { ArrowDown, ArrowLeft, ArrowUp, LoaderCircle, X } from "lucide-react"
import { useNavigate, useSearchParams } from "react-router-dom"

import { BottomNav } from "@/components/layout/bottom-nav"
import { BrandInsightCard } from "@/components/ui/brand-insight-card"
import { fetchPropertyComparisons, updatePropertyPriorities } from "@/features/properties/api/properties-api"
import type { PropertyComparison } from "@/features/properties/api/properties-api"
import { cn } from "@/lib/utils"

type ComparisonItem = PropertyComparison
type LoadState = "loading" | "success" | "error"

const mutedColumnColors = ["#aeb7bc", "#c6cdd1", "#d9dfe2"]

function formatYen(value: number) {
  return `¥${value.toLocaleString("en-US")}`
}

function formatMan(value: number) {
  return `¥${(value / 10_000).toFixed(value % 10_000 === 0 ? 0 : 1)}만`
}

function ComparisonBars({ items, field, highlightField, formatter }: { items: ComparisonItem[]; field: "livingMonths" | "initialSettlementCost"; highlightField: "longestLivingMonths" | "lowestInitialSettlementCost"; formatter: (value: number) => string }) {
  const maximum = Math.max(...items.map((item) => item[field]), 1)
  return (
    <div className="flex h-32 items-end justify-around gap-3 border-b border-[#dce3e6] px-2 pt-5">
      {items.map((item, index) => (
        <div key={item.propertyId} className="flex h-full min-w-0 flex-1 flex-col items-center justify-end gap-1.5">
          <span className="text-[11px] font-bold">{formatter(item[field])}</span>
          <span className="w-6 rounded-t-md" style={{ height: `${Math.max(20, (item[field] / maximum) * 72)}px`, backgroundColor: item[highlightField] ? "var(--brand)" : mutedColumnColors[index] }} />
          <span className="w-full truncate text-center text-[11px] text-[var(--text-secondary)]">{String.fromCharCode(65 + index)}</span>
        </div>
      ))}
    </div>
  )
}

function getInitialPriorityIds(items: ComparisonItem[]) {
  const savedPriorities = items.filter((item) => item.priorityRank !== null)
  if (savedPriorities.length > 0) {
    return [...items]
      .sort((left, right) => (left.priorityRank ?? Number.POSITIVE_INFINITY) - (right.priorityRank ?? Number.POSITIVE_INFINITY))
      .map((item) => item.propertyId)
  }

  const recommendedProperty = [...items].sort((left, right) =>
    (left.walkMinutes ?? Number.POSITIVE_INFINITY) - (right.walkMinutes ?? Number.POSITIVE_INFINITY)
    || (right.exclusiveAreaM2 ?? 0) - (left.exclusiveAreaM2 ?? 0),
  )[0]
  return [recommendedProperty.propertyId, ...items.filter((item) => item.propertyId !== recommendedProperty.propertyId).map((item) => item.propertyId)]
}

function ConditionComparison({ items }: { items: ComparisonItem[] }) {
  const comparisonGridStyle = { gridTemplateColumns: `96px repeat(${items.length}, minmax(0, 1fr))` }
  const knownWalkMinutes = items.map((item) => item.walkMinutes).filter((value): value is number => value !== null)
  const knownAreas = items.map((item) => item.exclusiveAreaM2).filter((value): value is number => value !== null)
  const minimumWalkMinutes = knownWalkMinutes.length > 0 ? Math.min(...knownWalkMinutes) : null
  const maximumArea = knownAreas.length > 0 ? Math.max(...knownAreas) : null
  const minimumUnknownCount = Math.min(...items.map((item) => item.unknownCostItemCount))
  const hasUnknownCostInformation = items.some((item) => item.unknownCostItemCount > 0)
  const balancedProperty = [...items].sort((left, right) =>
    (left.walkMinutes ?? Number.POSITIVE_INFINITY) - (right.walkMinutes ?? Number.POSITIVE_INFINITY)
    || (right.exclusiveAreaM2 ?? 0) - (left.exclusiveAreaM2 ?? 0),
  )[0]
  const balancedPropertyDetail = balancedProperty.walkMinutes === null || balancedProperty.exclusiveAreaM2 === null
    ? "역 도보와 전용면적을 함께 비교했어요."
    : `역 도보 ${balancedProperty.walkMinutes}분, 전용면적 ${balancedProperty.exclusiveAreaM2.toFixed(1)}㎡로 이동과 공간의 균형이 좋아요.`
  const rows = [
    { label: "지역", value: (item: ComparisonItem) => item.location ?? "미확인", highlighted: () => false },
    { label: "가까운 역", value: (item: ComparisonItem) => item.nearestStation ?? "미확인", highlighted: () => false },
    { label: "역 도보", value: (item: ComparisonItem) => item.walkMinutes === null ? "미확인" : `${item.walkMinutes}분`, highlighted: (item: ComparisonItem) => item.walkMinutes !== null && item.walkMinutes === minimumWalkMinutes },
    { label: "전용면적", value: (item: ComparisonItem) => item.exclusiveAreaM2 === null ? "미확인" : `${item.exclusiveAreaM2.toFixed(1)}㎡`, highlighted: (item: ComparisonItem) => item.exclusiveAreaM2 !== null && item.exclusiveAreaM2 === maximumArea },
    { label: "계약기간", value: (item: ComparisonItem) => item.contractPeriodMonths === null ? "미확인" : `${item.contractPeriodMonths / 12}년`, highlighted: () => false },
    ...(hasUnknownCostInformation ? [{ label: "미확인 정보", value: (item: ComparisonItem) => `${item.unknownCostItemCount}개`, highlighted: (item: ComparisonItem) => item.unknownCostItemCount === minimumUnknownCount }] : []),
  ]

  return (
    <div className="py-4">
      <section className="bg-white px-4 py-5">
        <BrandInsightCard title={<span className="text-[var(--brand)]">타지살이가 조건을 정리했어요</span>}>
          <p className="text-sm font-bold leading-5 text-[#1e2226]"><strong className="text-[#ef5350]">{balancedProperty.name}</strong> 조건이 가장 균형적이에요 <span className="inline-flex rounded-full bg-[var(--brand)] px-2 py-0.5 align-middle text-[11px] font-bold leading-5 text-white">추천</span></p>
          <p className="mt-1.5">{balancedPropertyDetail}</p>
        </BrandInsightCard>
        <h2 className="mt-7 text-base font-bold">조건 비교</h2>
        <div className="mt-4">
          <div className="grid text-center text-[11px] font-bold" style={comparisonGridStyle}>
            <span />
            {items.map((item, index) => <span key={item.propertyId} className="min-w-0 px-1 text-[var(--text-secondary)]"><span className="mb-2 block text-[10px] font-bold">{String.fromCharCode(65 + index)}</span><span className={cn("block py-1.5", item.propertyId === balancedProperty.propertyId && "rounded-t-lg bg-[#f2f7f7]")}>{item.thumbnailUrl && <img src={item.thumbnailUrl} alt="" className="mx-auto mb-1 size-12 rounded-xl object-cover" />}{item.name.replace(" 원룸", "").replace(" 스튜디오", "")}</span></span>)}
          </div>
          {rows.map((row, rowIndex) => <div key={row.label} className="grid text-center text-xs" style={comparisonGridStyle}><span className="px-2 py-3 text-left font-medium text-[var(--text-secondary)]">{row.label}</span>{items.map((item) => <span key={item.propertyId} className={cn("px-1 py-3 font-bold", row.highlighted(item) && "text-[var(--brand)]", item.propertyId === balancedProperty.propertyId && "bg-[#f2f7f7]", item.propertyId === balancedProperty.propertyId && rowIndex === rows.length - 1 && "rounded-b-lg")}>{row.value(item)}</span>)}</div>)}
        </div>
      </section>
    </div>
  )
}

function PropertyComparisonPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [searchParams] = useSearchParams()
  const [state, setState] = useState<LoadState>("loading")
  const [items, setItems] = useState<ComparisonItem[]>([])
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [activeTab, setActiveTab] = useState<"funds" | "notes">("funds")
  const [prioritySheetOpen, setPrioritySheetOpen] = useState(false)
  const [priorityIds, setPriorityIds] = useState<string[]>([])
  const [isSavingPriorities, setIsSavingPriorities] = useState(false)
  const [prioritySaveError, setPrioritySaveError] = useState<string | null>(null)
  const selectedIds = useMemo(() => [...new Set((searchParams.get("ids") ?? "").split(",").filter(Boolean))].slice(0, 3), [searchParams])
  const shouldOpenPrioritySheet = searchParams.get("priority") === "true"
  const hasValidSelection = selectedIds.length >= 2
  const displayState = hasValidSelection ? state : "error"

  useEffect(() => {
    let active = true
    if (!hasValidSelection) return () => { active = false }

    fetchPropertyComparisons(selectedIds)
      .then((comparisons) => {
        if (!active) return
        if (comparisons.length < 2) {
          setErrorMessage("비교 결과에 매물이 충분하지 않아요.")
          setState("error")
          return
        }
        setItems(comparisons)
        setErrorMessage(null)
        setState("success")
        if (shouldOpenPrioritySheet) {
          setPriorityIds(getInitialPriorityIds(comparisons))
          setPrioritySaveError(null)
          setPrioritySheetOpen(true)
        }
      })
      .catch((error: unknown) => {
        if (!active) return
        setErrorMessage(error instanceof Error ? error.message : "매물 비교에 실패했어요. 다시 시도해주세요.")
        setState("error")
      })

    return () => { active = false }
  }, [hasValidSelection, selectedIds, shouldOpenPrioritySheet])

  const lowestInitialCost = items.find((item) => item.lowestInitialSettlementCost) ?? null
  const greatestBalanceAfterMoveIn = Math.max(...items.map((item) => item.balanceAfterMoveIn))
  const lowestNonRefundableCost = Math.min(...items.map((item) => item.nonRefundableCost))
  const greatestRefundableDeposit = Math.max(...items.map((item) => item.refundableDeposit))
  const comparisonGridStyle = { gridTemplateColumns: `96px repeat(${items.length}, minmax(0, 1fr))` }

  function openPrioritySheet() {
    setPriorityIds(getInitialPriorityIds(items))
    setPrioritySaveError(null)
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

  async function savePriorities() {
    setIsSavingPriorities(true)
    setPrioritySaveError(null)
    try {
      await updatePropertyPriorities(priorityIds[0] ?? null, priorityIds[1] ?? null)
      setPrioritySheetOpen(false)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["home-progress"] }),
        queryClient.invalidateQueries({ queryKey: ["priority-properties"] }),
      ])
      void navigate("/home")
    } catch (error) {
      setPrioritySaveError(error instanceof Error ? error.message : "우선순위를 저장하지 못했어요. 다시 시도해주세요.")
    } finally {
      setIsSavingPriorities(false)
    }
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
          <button type="button" onClick={() => setActiveTab("notes")} className={cn("min-h-12 border-b-2", activeTab === "notes" ? "border-[var(--brand)] text-[var(--brand)]" : "border-transparent text-[var(--text-secondary)]")}>조건 비교</button>
        </div>
      </div>

      {displayState === "loading" && <div className="flex min-h-80 flex-col items-center justify-center gap-3"><LoaderCircle className="size-7 animate-spin text-[var(--brand)]" /><p className="text-sm font-semibold text-[var(--text-secondary)]">비교 결과를 불러오고 있어요</p></div>}
      {displayState === "error" && <div className="flex min-h-80 flex-col items-center justify-center px-6 text-center"><p className="text-base font-bold">{errorMessage ?? "비교할 매물을 2개 이상 선택해주세요"}</p><button type="button" onClick={() => void navigate("/properties?tab=compare")} className="mt-4 h-11 rounded-lg bg-[var(--brand)] px-5 text-sm font-bold text-white">매물 선택하기</button></div>}

      {displayState === "success" && activeTab === "funds" && (
        <div className="space-y-3 py-4">
          <section className="bg-white px-4 py-5">
            <h2 className="text-base font-bold">비교 요약</h2>
            <div className="mt-1 grid grid-cols-2 divide-x divide-[#edf0f2]">
              <div className="px-2 py-2"><ComparisonBars items={items} field="livingMonths" highlightField="longestLivingMonths" formatter={(value) => value.toFixed(1)} /><p className="pt-2 text-center text-[11px] text-[var(--text-secondary)]">생활 가능기간 비교</p></div>
              <div className="px-2 py-2"><ComparisonBars items={items} field="initialSettlementCost" highlightField="lowestInitialSettlementCost" formatter={(value) => `${Math.round(value / 1000)}K`} /><p className="pt-2 text-center text-[11px] text-[var(--text-secondary)]">초기 정착비 비교</p></div>
            </div>
            {lowestInitialCost && <BrandInsightCard className="mt-4" title={<>비용 부담은 <span className="text-[#ef5350]">{lowestInitialCost.name.replace(" 스튜디오", "").replace(" 원룸", "")}</span>가 가장 낮아요</>}>생활 가능기간은 가장 길고, 초기 정착비는 가장 적어요.</BrandInsightCard>}
          </section>

          <section className="bg-white px-4 py-5">
            <h2 className="text-base font-bold">핵심 비교</h2>
            <div className="mt-3">
              <div className="grid text-center text-[11px] font-bold" style={comparisonGridStyle}>
                <span />
                {items.map((item, index) => <span key={item.propertyId} className="min-w-0 px-1 text-[var(--text-secondary)]"><span className="mb-2 block text-[10px] font-bold">{String.fromCharCode(65 + index)}</span><span className={cn("block py-1.5", item.lowestInitialSettlementCost && "rounded-t-lg bg-[#f2f7f7]")}>{item.thumbnailUrl && <img src={item.thumbnailUrl} alt="" className="mx-auto mb-1 size-12 rounded-xl object-cover" />}{item.name.replace(" 원룸", "").replace(" 스튜디오", "")}</span></span>)}
              </div>
              {([
                ["월 주거비", (item: ComparisonItem) => formatYen(item.monthlyHousingCost), "lowestMonthlyHousingCost"],
                ["초기 정산", (item: ComparisonItem) => formatYen(item.initialSettlementCost), "lowestInitialSettlementCost"],
                ["입주 후 잔액", (item: ComparisonItem) => formatMan(item.balanceAfterMoveIn), null],
                ["생활 가능", (item: ComparisonItem) => item.unlimited ? "무제한" : `${item.livingMonths.toFixed(1)}개월`, "longestLivingMonths"],
              ] as Array<[string, (item: ComparisonItem) => string, "lowestMonthlyHousingCost" | "lowestInitialSettlementCost" | "longestLivingMonths" | null]>).map(([label, formatter, highlightField]) => <div key={label} className="grid text-center text-xs" style={comparisonGridStyle}><span className="px-2 py-3 text-left font-medium text-[var(--text-secondary)]">{label}</span>{items.map((item) => <span key={item.propertyId} className={cn("px-1 py-3 font-bold", item.lowestInitialSettlementCost && "bg-[#f2f7f7]", (highlightField && item[highlightField] || label === "입주 후 잔액" && item.balanceAfterMoveIn === greatestBalanceAfterMoveIn) && "text-[var(--brand)]")}>{formatter(item)}</span>)}</div>)}
            </div>
          </section>

          <section className="bg-white px-4 py-5"><h2 className="text-base font-bold">초기비용 구성</h2><div className="mt-3">{[["비반환 비용", "nonRefundableCost"], ["반환 가능 보증금", "refundableDeposit"]].map(([label, field]) => <div key={label} className="grid text-center text-xs" style={comparisonGridStyle}><span className="px-2 py-2 text-left font-medium text-[var(--text-secondary)]">{label}</span>{items.map((item) => { const value = item[field as "refundableDeposit" | "nonRefundableCost"]; const isBest = field === "nonRefundableCost" ? value === lowestNonRefundableCost : value === greatestRefundableDeposit; return <span key={item.propertyId} className={cn("px-1 py-2 font-bold", item.lowestInitialSettlementCost && "bg-[#f2f7f7]", isBest && "text-[var(--brand)]")}>{formatYen(value).replace(",000", "K")}</span>})}</div>)}</div><p className="mt-4 text-[11px] leading-5 text-[var(--text-secondary)]">계약 시 필요한 현금을 반환 여부로 나눠 비교해요.</p></section>
        </div>
      )}

      {displayState === "success" && activeTab === "notes" && <ConditionComparison items={items} />}

      {displayState === "success" && <div className="fixed inset-x-0 bottom-[calc(100px+env(safe-area-inset-bottom))] z-20 mx-auto w-full max-w-[430px] px-4"><button type="button" onClick={openPrioritySheet} className="flex h-12 w-full items-center justify-center rounded-lg bg-[var(--brand)] text-sm font-bold text-white shadow-sm">매물 우선순위 정하기</button></div>}
      <BottomNav />
      {prioritySheetOpen && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/30" role="presentation" onMouseDown={() => { if (!isSavingPriorities) setPrioritySheetOpen(false) }}>
          <section role="dialog" aria-modal="true" aria-labelledby="priority-sheet-title" className="w-full max-w-[430px] rounded-t-3xl bg-white px-5 pb-[calc(20px+env(safe-area-inset-bottom))] pt-3 shadow-2xl" onMouseDown={(event) => event.stopPropagation()}>
            <div className="mx-auto h-1 w-10 rounded-full bg-[#dde2e5]" />
            <div className="mt-5 flex items-start justify-between"><div><h2 id="priority-sheet-title" className="text-lg font-bold">매물 우선순위 정하기</h2><p className="mt-1 text-xs text-[var(--text-secondary)]">마음에 드는 순서대로 정렬해주세요. 1·2순위만 저장돼요.</p></div><button type="button" disabled={isSavingPriorities} onClick={() => setPrioritySheetOpen(false)} className="grid size-10 place-items-center text-[var(--text-secondary)] disabled:opacity-40" aria-label="닫기"><X className="size-5" /></button></div>
            <ol className="mt-5 divide-y divide-[#edf0f2] rounded-xl border border-[#e7ebed]">
              {priorityIds.map((id, index) => { const item = items.find((candidate) => candidate.propertyId === id); if (!item) return null; return <li key={id} className="flex items-center gap-3 px-3 py-3"><span className={cn("grid size-7 shrink-0 place-items-center rounded-full text-sm font-bold", index === 0 ? "bg-[var(--brand)] text-white" : "bg-[#eef1f2] text-[var(--text-secondary)]")}>{index + 1}</span>{item.thumbnailUrl && <img src={item.thumbnailUrl} alt="" className="size-12 rounded-lg object-cover" />}<span className="min-w-0 flex-1 truncate text-sm font-bold">{item.name}</span><div className="flex items-center gap-0.5"><button type="button" disabled={isSavingPriorities || index === 0} onClick={() => movePriority(id, -1)} className="grid size-9 place-items-center rounded-lg text-[var(--text-secondary)] disabled:opacity-25" aria-label="위로 이동"><ArrowUp className="size-4" /></button><button type="button" disabled={isSavingPriorities || index === priorityIds.length - 1} onClick={() => movePriority(id, 1)} className="grid size-9 place-items-center rounded-lg text-[var(--text-secondary)] disabled:opacity-25" aria-label="아래로 이동"><ArrowDown className="size-4" /></button></div></li> })}
            </ol>
            {prioritySaveError && <p className="mt-3 text-center text-xs font-medium text-[#ef5350]">{prioritySaveError}</p>}
            <button type="button" disabled={isSavingPriorities} onClick={() => void savePriorities()} className="mt-5 flex h-12 w-full items-center justify-center rounded-lg bg-[var(--brand)] text-sm font-bold text-white disabled:opacity-60">{isSavingPriorities ? "저장 중..." : "이 순서로 저장하기"}</button>
          </section>
        </div>
      )}
    </main>
  )
}

export { PropertyComparisonPage }
