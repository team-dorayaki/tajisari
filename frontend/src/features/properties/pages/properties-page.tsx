import { useEffect, useState } from "react"
import { Check, ChevronRight, ImageOff, LoaderCircle, Plus, Trash2 } from "lucide-react"
import { useNavigate } from "react-router-dom"

import { BottomNav } from "@/components/layout/bottom-nav"
import { deleteProperties, fetchProperties } from "@/features/properties/api/properties-api"
import type { PropertySummary } from "@/features/properties/api/properties-api"
import { PropertyDeleteDialog } from "@/features/properties/components/property-delete-dialog"
import { cn } from "@/lib/utils"

type ListStatus = "loading" | "success" | "error"

function PropertiesPage() {
  const navigate = useNavigate()
  const [properties, setProperties] = useState<PropertySummary[]>([])
  const [status, setStatus] = useState<ListStatus>("loading")
  const [selectionMode, setSelectionMode] = useState(false)
  const [selectedIds, setSelectedIds] = useState<string[]>([])
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [failedThumbnailIds, setFailedThumbnailIds] = useState<string[]>([])

  async function loadProperties() {
    setStatus("loading")
    try {
      setProperties(await fetchProperties())
      setStatus("success")
    } catch {
      setStatus("error")
    }
  }

  useEffect(() => {
    let active = true
    void fetchProperties()
      .then((response) => {
        if (!active) return
        setProperties(response)
        setStatus("success")
      })
      .catch(() => {
        if (active) setStatus("error")
      })

    return () => { active = false }
  }, [])

  function closeSelectionMode() {
    setSelectionMode(false)
    setSelectedIds([])
  }

  function toggleProperty(propertyId: string) {
    setSelectedIds((current) => current.includes(propertyId)
      ? current.filter((id) => id !== propertyId)
      : [...current, propertyId])
  }

  async function confirmDelete() {
    setIsDeleting(true)
    setDeleteError(null)
    try {
      await deleteProperties(selectedIds)
      setProperties((current) => current.filter((property) => !selectedIds.includes(property.id)))
      setDeleteDialogOpen(false)
      closeSelectionMode()
    } catch {
      setDeleteError("매물을 삭제하지 못했어요. 다시 시도해주세요.")
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <main className="flex min-h-dvh flex-col bg-[#f5f6f7] pb-[calc(164px+env(safe-area-inset-bottom))]">
      <header className="grid h-[calc(72px+env(safe-area-inset-top))] grid-cols-[64px_1fr_64px] items-center bg-white px-2 pt-[env(safe-area-inset-top)]">
        <span />
        <h1 className="text-center text-base font-bold">매물</h1>
        {status === "success" && properties.length > 0 && (
          <button type="button" onClick={() => selectionMode ? closeSelectionMode() : setSelectionMode(true)} className="min-h-11 text-sm font-bold text-[var(--brand)]">
            {selectionMode ? "취소" : "선택"}
          </button>
        )}
      </header>

      <div className="px-4 pt-3">
        <div className="grid grid-cols-2 rounded-lg bg-[#e8edee] p-1 text-xs font-semibold">
          <button type="button" className="min-h-10 rounded-md bg-white text-[var(--brand)] shadow-sm">매물 리스트</button>
          <button type="button" disabled={selectionMode} onClick={() => void navigate("/properties/compare")} className="min-h-10 rounded-md text-[var(--text-secondary)] disabled:opacity-50">비교</button>
        </div>
      </div>

      <section className="flex-1 pt-5" aria-labelledby="registered-properties-title">
        <div className="flex items-center justify-between px-4 pb-3">
          <h2 id="registered-properties-title" className="text-base font-bold">{selectionMode ? "삭제할 매물 선택" : "등록한 매물"}</h2>
          <span className="text-xs text-[var(--text-secondary)]">{selectionMode ? `${selectedIds.length}개 선택` : `${properties.length}개`}</span>
        </div>

        {status === "loading" && (
          <div className="flex min-h-52 flex-col items-center justify-center gap-3" aria-live="polite">
            <LoaderCircle aria-hidden="true" className="size-7 animate-spin text-[var(--brand)]" />
            <p className="text-sm font-semibold text-[var(--text-secondary)]">매물 목록을 불러오고 있어요</p>
          </div>
        )}

        {status === "error" && (
          <div className="flex min-h-52 flex-col items-center justify-center px-4 text-center" role="alert">
            <p className="text-sm font-bold">매물 목록을 불러오지 못했어요</p>
            <button type="button" onClick={() => void loadProperties()} className="mt-4 h-11 rounded-lg bg-[var(--brand)] px-5 text-sm font-bold text-white">다시 시도하기</button>
          </div>
        )}

        {status === "success" && properties.length === 0 && (
          <div className="flex min-h-52 flex-col items-center justify-center px-4 text-center">
            <ImageOff aria-hidden="true" className="size-9 text-[#98a1aa]" strokeWidth={1.5} />
            <p className="mt-3 text-sm font-bold">등록한 매물이 없어요</p>
            <p className="mt-1 text-xs text-[var(--text-secondary)]">관심 있는 매물을 등록해보세요.</p>
          </div>
        )}

        {status === "success" && properties.length > 0 && (
          <div className="divide-y divide-[#edf0f2] bg-white">
            {properties.map((property) => {
              const selected = selectedIds.includes(property.id)
              return (
                <button
                  key={property.id}
                  type="button"
                  onClick={() => selectionMode ? toggleProperty(property.id) : void navigate(`/properties/${property.id}`)}
                  className={cn("flex w-full items-center gap-3 px-4 py-4 text-left", selected && "bg-[var(--brand-soft)]")}
                  aria-label={selectionMode ? `${property.name} ${selected ? "선택 해제" : "선택"}` : `${property.name} 상세 보기`}
                  aria-pressed={selectionMode ? selected : undefined}
                >
                  {selectionMode && (
                    <span className={cn("grid size-6 shrink-0 place-items-center rounded-md border-2", selected ? "border-[var(--brand)] bg-[var(--brand)] text-white" : "border-[#b8c0c7] bg-white")}>
                      {selected && <Check aria-hidden="true" className="size-4" strokeWidth={3} />}
                    </span>
                  )}
                  <span className="grid size-[76px] shrink-0 place-items-center overflow-hidden rounded-lg bg-[#e7edef]">
                    {property.images[0] && !failedThumbnailIds.includes(property.images[0].id) ? (
                      <img src={property.images[0].src} alt="" className="h-full w-full object-cover" onError={() => setFailedThumbnailIds((failed) => [...failed, property.images[0].id])} />
                    ) : (
                      <ImageOff aria-hidden="true" className="size-7 text-[#7c8790]" strokeWidth={1.5} />
                    )}
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-sm font-bold">{property.name}</span>
                    <span className="mt-1 block text-xs font-semibold">월 ¥{property.rent.toLocaleString("en-US")}</span>
                    <span className="mt-1 block text-[11px] text-[var(--text-secondary)]">초기비용 ¥{property.initialCost.toLocaleString("en-US")}</span>
                    <span className="block text-[11px] font-bold text-[var(--brand)]">생활 가능 {property.livingMonths}</span>
                  </span>
                  {!selectionMode && <ChevronRight aria-hidden="true" className="size-5 text-[#98a1aa]" />}
                </button>
              )
            })}
          </div>
        )}
      </section>

      <div className="fixed inset-x-0 bottom-[calc(100px+env(safe-area-inset-bottom))] z-20 mx-auto w-full max-w-[430px] px-4">
        {selectionMode ? (
          <button type="button" disabled={selectedIds.length === 0} onClick={() => { setDeleteError(null); setDeleteDialogOpen(true) }} className="flex h-12 w-full items-center justify-center gap-2 rounded-lg bg-[#e45f4c] text-sm font-bold text-white shadow-sm disabled:bg-[#d9dfe4]">
            <Trash2 aria-hidden="true" className="size-4" /> 선택한 매물 삭제
          </button>
        ) : (
          <button type="button" onClick={() => void navigate("/properties/new")} className="flex h-12 w-full items-center justify-center gap-1 rounded-lg bg-[var(--brand)] text-sm font-bold text-white shadow-sm">
            <Plus aria-hidden="true" className="size-4" /> 매물 등록하기
          </button>
        )}
      </div>

      <BottomNav />
      {deleteDialogOpen && <PropertyDeleteDialog count={selectedIds.length} isDeleting={isDeleting} error={deleteError} onCancel={() => setDeleteDialogOpen(false)} onConfirm={() => void confirmDelete()} />}
    </main>
  )
}

export { PropertiesPage }
