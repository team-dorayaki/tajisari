import { useEffect, useState } from "react"
import { ArrowLeft, ChevronLeft, ChevronRight, EllipsisVertical, ImageOff, Landmark, LoaderCircle, ReceiptText, Trash2 } from "lucide-react"
import { useNavigate, useParams } from "react-router-dom"

import { BottomNav } from "@/components/layout/bottom-nav"
import { deleteProperties, fetchProperty } from "@/features/properties/api/properties-api"
import { PropertyFinancialSimulation } from "@/features/properties/components/property-financial-simulation"
import { PropertyDeleteDialog } from "@/features/properties/components/property-delete-dialog"
import type { PropertyDetail, PropertyDetailCostItem, PropertyImage } from "@/features/properties/api/properties-api"

type DisplayCost = PropertyDetailCostItem

function formatYen(amount: number) {
  return `¥${amount.toLocaleString("en-US")}`
}

function CostRow({ item }: { item: DisplayCost }) {
  return (
    <div className="border-b border-[#eef0f2] py-3 last:border-b-0">
      <div className="flex min-h-8 items-center gap-2">
        <span className="min-w-0 flex-1 truncate text-[13px] font-medium">{item.label}</span>
        <strong className="shrink-0 text-[13px] tabular-nums">{item.amount === null ? "금액 미정" : formatYen(item.amount)}</strong>
      </div>
    </div>
  )
}

function PropertyImageCarousel({ images, propertyName }: { images: PropertyImage[]; propertyName: string }) {
  const [activeIndex, setActiveIndex] = useState(0)
  const [failedImageIds, setFailedImageIds] = useState<string[]>([])
  const availableImages = images.filter((image) => !failedImageIds.includes(image.id))
  const hasImages = availableImages.length > 0
  const normalizedIndex = hasImages ? activeIndex % availableImages.length : 0
  const activeImage = availableImages[normalizedIndex]

  function showPreviousImage() {
    setActiveIndex((current) => (current - 1 + availableImages.length) % availableImages.length)
  }

  function showNextImage() {
    setActiveIndex((current) => (current + 1) % availableImages.length)
  }

  return (
    <section className="relative h-[200px] overflow-hidden bg-[#f1f3f4]" aria-label={`${propertyName} 매물 사진`}>
      {activeImage ? (
        <img
          key={activeImage.id}
          src={activeImage.src}
          alt={activeImage.alt}
          className="h-full w-full object-cover"
          onError={() => setFailedImageIds((failed) => [...failed, activeImage.id])}
        />
      ) : (
        <div className="flex h-full flex-col items-center justify-center gap-2 text-[#98a1aa]">
          <ImageOff aria-hidden="true" className="size-8" strokeWidth={1.5} />
          <span className="text-xs">등록된 매물 사진이 없어요</span>
        </div>
      )}

      {availableImages.length > 1 && (
        <>
          <button type="button" onClick={showPreviousImage} className="absolute top-1/2 left-3 grid size-10 -translate-y-1/2 place-items-center rounded-full bg-black/35 text-white" aria-label="이전 사진">
            <ChevronLeft aria-hidden="true" className="size-5" />
          </button>
          <button type="button" onClick={showNextImage} className="absolute top-1/2 right-3 grid size-10 -translate-y-1/2 place-items-center rounded-full bg-black/35 text-white" aria-label="다음 사진">
            <ChevronRight aria-hidden="true" className="size-5" />
          </button>
          <span className="absolute right-3 bottom-3 rounded-full bg-black/55 px-2.5 py-1 text-[11px] font-semibold tabular-nums text-white" aria-label={`사진 ${normalizedIndex + 1} / ${availableImages.length}`}>
            {normalizedIndex + 1}/{availableImages.length}
          </span>
        </>
      )}
    </section>
  )
}

function PropertyDetailPage() {
  const navigate = useNavigate()
  const { propertyId = "" } = useParams()
  const [property, setProperty] = useState<PropertyDetail | null>(null)
  const [status, setStatus] = useState<"loading" | "success" | "error">("loading")
  const [activeTab, setActiveTab] = useState<"costs" | "simulation">("costs")
  const [menuOpen, setMenuOpen] = useState(false)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    window.scrollTo(0, 0)
    void fetchProperty(propertyId)
      .then((response) => {
        if (!active) return
        setProperty(response)
        setStatus("success")
      })
      .catch(() => {
        if (active) setStatus("error")
      })
    return () => { active = false }
  }, [propertyId])

  const analysis = property?.costAnalysis
  const summary = analysis?.summary
  const recurringCosts: DisplayCost[] = analysis?.costGroups.monthly ?? []
  const moveInCosts: DisplayCost[] = analysis?.costGroups.moveIn ?? []
  const selectionCosts: DisplayCost[] = analysis?.costGroups.optional ?? []
  const futureCosts: DisplayCost[] = analysis?.costGroups.future ?? []
  const estimatedExitCost = summary?.estimatedMoveOutCost ?? 0
  const estimatedInitialCost = summary?.initialCost ?? 0
  const refundableAmount = summary?.refundableAmount ?? 0
  const nonRefundableAmount = summary?.nonRefundableAmount ?? 0
  const monthlyCost = summary?.monthlyCost ?? 0
  const selectionCost = summary?.selectedOptionalCost ?? 0
  const minimumInitialCost = summary?.minimumInitialCost ?? 0
  const contractCost = summary?.contractMoveInCost ?? 0
  const graphTotal = Math.max(monthlyCost + contractCost + selectionCost, 1)
  const monthlyGraphWidth = `${(monthlyCost / graphTotal) * 100}%`
  const contractGraphWidth = `${(contractCost / graphTotal) * 100}%`
  const selectionGraphWidth = `${(selectionCost / graphTotal) * 100}%`
  async function confirmDelete() {
    setIsDeleting(true)
    setDeleteError(null)
    try {
      await deleteProperties([propertyId])
      void navigate("/properties", { replace: true })
    } catch {
      setDeleteError("매물을 삭제하지 못했어요. 다시 시도해주세요.")
      setIsDeleting(false)
    }
  }

  if (status === "loading") return <main className="flex min-h-dvh items-center justify-center bg-[#f5f6f7]" aria-live="polite"><div className="flex flex-col items-center gap-3 text-[var(--text-secondary)]"><LoaderCircle className="size-7 animate-spin text-[var(--brand)]" aria-hidden="true" /><p className="text-sm font-semibold">매물 상세를 불러오고 있어요</p></div></main>

  if (status === "error" || property === null) return <main className="flex min-h-dvh flex-col items-center justify-center bg-[#f5f6f7] px-6 text-center" role="alert"><h1 className="text-base font-bold">매물 정보를 불러오지 못했어요</h1><p className="mt-2 text-sm text-[var(--text-secondary)]">매물이 삭제되었거나 잠시 문제가 발생했어요.</p><button type="button" onClick={() => void navigate("/properties", { replace: true })} className="mt-5 h-11 rounded-lg bg-[var(--brand)] px-5 text-sm font-bold text-white">매물 목록으로 돌아가기</button></main>

  return (
    <main className="min-h-dvh bg-[#f5f6f7] pb-[calc(116px+env(safe-area-inset-bottom))]" aria-label="매물 상세 비용 분석">
      <header className="sticky top-0 z-20 bg-white pt-[env(safe-area-inset-top)]">
        <div className="relative grid h-14 grid-cols-[44px_1fr_44px] items-center px-2">
          <button type="button" onClick={() => void navigate("/properties", { replace: true })} className="grid size-11 place-items-center rounded-full" aria-label="매물 목록으로 돌아가기"><ArrowLeft aria-hidden="true" className="size-5" /></button>
          <h1 className="truncate px-2 text-center text-sm font-bold">{property.name}</h1>
          <button type="button" onClick={() => setMenuOpen((open) => !open)} className="grid size-11 place-items-center rounded-full" aria-label="매물 더보기" aria-expanded={menuOpen}><EllipsisVertical aria-hidden="true" className="size-5" /></button>
          {menuOpen && (
            <div className="absolute top-12 right-2 z-30 w-36 rounded-xl border border-[#e5e9ec] bg-white p-1 shadow-lg" role="menu">
              <button type="button" role="menuitem" onClick={() => { setMenuOpen(false); setDeleteError(null); setDeleteDialogOpen(true) }} className="flex min-h-11 w-full items-center gap-2 rounded-lg px-3 text-left text-sm font-semibold text-[#e45f4c] hover:bg-[#fff0ed]">
                <Trash2 aria-hidden="true" className="size-4" /> 매물 삭제
              </button>
            </div>
          )}
        </div>
      </header>
      <PropertyImageCarousel images={property.images} propertyName={property.name} />
      <section className="bg-white px-4 pt-4"><h2 className="text-base font-bold">{property.name}</h2><p className="mt-1 text-[11px] text-[var(--text-secondary)]">월세 {formatYen(property.property.rent ?? 0)} · 관리비 {formatYen(property.property.managementFee ?? 0)}</p><p className="mt-1 text-[11px] text-[var(--text-secondary)]">{property.areaLabel} · {property.property.availableFrom?.replaceAll("-", ".") ?? "입주일 미정"} 입주 가능</p><div className="mt-5 grid grid-cols-2 text-center text-xs font-semibold"><button type="button" onClick={() => setActiveTab("costs")} className={`border-b-2 pb-3 ${activeTab === "costs" ? "border-[var(--brand)] text-[var(--foreground)]" : "border-transparent text-[var(--text-secondary)]"}`}>비용 분석</button><button type="button" onClick={() => setActiveTab("simulation")} className={`border-b-2 pb-3 ${activeTab === "simulation" ? "border-[var(--brand)] text-[var(--foreground)]" : "border-transparent text-[var(--text-secondary)]"}`}>자금 시뮬레이션</button></div></section>
      {activeTab === "simulation" ? property.simulation ? <PropertyFinancialSimulation simulation={property.simulation} estimatedExitCost={estimatedExitCost} stayMonths={property.simulation.plannedStayMonths} /> : <section className="m-4 rounded-2xl bg-white p-6 text-center"><h2 className="text-base font-bold">정착 계획을 작성하면 자금 시뮬레이션을 볼 수 있어요</h2><button type="button" onClick={() => void navigate("/settlement-plan")} className="mt-4 h-11 rounded-lg bg-[var(--brand)] px-5 text-sm font-bold text-white">정착 계획 작성하기</button></section> : <>
      <section className="border-b-8 border-[#f5f6f7] bg-white px-4 py-5"><p className="text-xs text-[var(--text-secondary)]">예상 초기 정착비</p><div className="mt-1 flex items-end justify-between gap-3"><strong className="text-2xl tracking-[-0.03em] tabular-nums">{formatYen(estimatedInitialCost)}</strong><span className="mb-1 text-[11px] text-[var(--text-secondary)]">선택 비용 제외 최소 <strong className="font-bold text-[var(--brand)]">{formatYen(minimumInitialCost)}</strong></span></div><div className="mt-4 flex h-2 overflow-hidden rounded-full bg-[#e2e7e8]" aria-label={`매월 반복비용 ${formatYen(monthlyCost)}, 계약 입주 시 비용 ${formatYen(contractCost)}, 선택 비용 ${formatYen(selectionCost)}`}><span className="bg-[#15171c]" style={{ width: monthlyGraphWidth }} /><span className="bg-[var(--brand)]" style={{ width: contractGraphWidth }} /><span className="bg-[#dfe5e8]" style={{ width: selectionGraphWidth }} /></div><div className="mt-3 flex flex-wrap justify-between gap-x-3 gap-y-1 text-[10px] text-[var(--text-secondary)]"><span><i className="mr-1 inline-block size-2 rounded-full bg-[#15171c]" />매월 반복 {formatYen(monthlyCost)}</span><span><i className="mr-1 inline-block size-2 rounded-full bg-[var(--brand)]" />계약·입주 {formatYen(contractCost)}</span><span><i className="mr-1 inline-block size-2 rounded-full bg-[#dfe5e8]" />선택 {formatYen(selectionCost)}</span></div></section>
      <section className="border-b-8 border-[#f5f6f7] bg-white px-4 py-5"><div className="flex items-start gap-2"><span className="mt-1 size-2 rounded-full bg-[var(--brand)]" /><h2 className="text-sm font-bold">매월 반복비용</h2></div><div className="mt-3">{recurringCosts.map((item) => <CostRow key={item.id} item={item} />)}</div></section>
      <section className="border-b-8 border-[#f5f6f7] bg-white px-4 py-5"><div className="flex items-start gap-2"><span className="mt-1 size-2 rounded-full bg-[#15171c]" /><h2 className="text-sm font-bold">계약 · 입주 시 비용</h2></div><div className="mt-3">{moveInCosts.map((item) => <CostRow key={item.id} item={item} />)}</div></section>
      {selectionCosts.length > 0 && <section className="border-b-8 border-[#f5f6f7] bg-white px-4 py-5"><div className="flex items-start gap-2"><span className="mt-1 size-2 rounded-full bg-[#dfe5e8]" /><h2 className="text-sm font-bold">선택 비용</h2></div><div className="mt-3">{selectionCosts.map((item) => <CostRow key={item.id} item={item} />)}</div></section>}
      <section className="border-b-8 border-[#f5f6f7] bg-white px-4 py-5"><h2 className="text-base font-bold">반환 여부</h2><p className="mt-1 text-[11px] text-[var(--text-secondary)]">반환 가능 금액이며, 퇴거 시 청소·원상복구비가 차감될 수 있어요.</p><div className="mt-4 grid grid-cols-2 gap-3"><div className="flex min-h-20 items-center gap-2 rounded-2xl bg-[var(--brand)] px-4 text-white"><Landmark aria-hidden="true" className="size-5 shrink-0" /><strong className="text-sm tabular-nums">반환 가능&nbsp; {formatYen(refundableAmount)}</strong></div><div className="flex min-h-20 items-center gap-2 rounded-2xl bg-[#f7f8f9] px-4 text-[#15171c]"><ReceiptText aria-hidden="true" className="size-5 shrink-0 text-[#697480]" /><strong className="text-sm tabular-nums">비반환&nbsp; {formatYen(nonRefundableAmount)}</strong></div></div></section>
      <section className="border-b-8 border-[#f5f6f7] bg-white px-4 py-5"><div className="flex items-start gap-2"><span className="mt-1 size-2 rounded-full bg-[#e6ebee]" /><h2 className="text-sm font-bold">계약 후 비용</h2></div><div className="mt-3">{futureCosts.map((item) => <CostRow key={item.id} item={item} />)}</div></section>
      </>}
      <BottomNav />
      {deleteDialogOpen && <PropertyDeleteDialog count={1} isDeleting={isDeleting} error={deleteError} onCancel={() => setDeleteDialogOpen(false)} onConfirm={() => void confirmDelete()} />}
    </main>
  )
}

export { PropertyDetailPage }
