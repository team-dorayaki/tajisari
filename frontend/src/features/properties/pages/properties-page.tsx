import { useEffect, useState } from "react"
import { ChevronRight, ImageOff, Plus } from "lucide-react"
import { useNavigate } from "react-router-dom"

import { BottomNav } from "@/components/layout/bottom-nav"
import { fetchProperties } from "@/features/properties/api/properties-api"
import type { PropertySummary } from "@/features/properties/api/properties-api"

function PropertiesPage() {
  const navigate = useNavigate()
  const [properties, setProperties] = useState<PropertySummary[]>([])
  const [failedThumbnailIds, setFailedThumbnailIds] = useState<string[]>([])

  useEffect(() => {
    void fetchProperties().then(setProperties)
  }, [])

  return (
    <main className="flex min-h-dvh flex-col bg-[#f5f6f7] pb-[calc(164px+env(safe-area-inset-bottom))]">
      <header className="flex h-[calc(72px+env(safe-area-inset-top))] items-center justify-center bg-white pt-[env(safe-area-inset-top)]">
        <h1 className="text-base font-bold">매물</h1>
      </header>

      <div className="px-4 pt-3">
        <div className="grid grid-cols-2 rounded-lg bg-[#e8edee] p-1 text-xs font-semibold">
          <button type="button" className="min-h-10 rounded-md bg-white text-[var(--brand)] shadow-sm">
            매물 리스트
          </button>
          <button
            type="button"
            onClick={() => void navigate("/properties/compare")}
            className="min-h-10 rounded-md text-[var(--text-secondary)]"
          >
            비교
          </button>
        </div>
      </div>

      <section className="pt-5" aria-labelledby="registered-properties-title">
        <div className="flex items-center justify-between px-4 pb-3">
          <h2 id="registered-properties-title" className="text-base font-bold">등록한 매물</h2>
          <span className="text-xs text-[var(--text-secondary)]">{properties.length}개</span>
        </div>

        <div className="divide-y divide-[#edf0f2] bg-white">
          {properties.map((property) => (
            <button
              key={property.id}
              type="button"
              onClick={() => void navigate(`/properties/${property.id}`)}
              className="flex w-full items-center gap-3 px-4 py-4 text-left"
              aria-label={`${property.name} 상세 보기`}
            >
              <span className="grid size-[76px] shrink-0 place-items-center overflow-hidden rounded-lg bg-[#e7edef]">
                {property.images[0] && !failedThumbnailIds.includes(property.images[0].id) ? (
                  <img
                    src={property.images[0].src}
                    alt=""
                    className="h-full w-full object-cover"
                    onError={() => setFailedThumbnailIds((failed) => [...failed, property.images[0].id])}
                  />
                ) : (
                  <ImageOff aria-hidden="true" className="size-7 text-[#7c8790]" strokeWidth={1.5} />
                )}
              </span>
              <span className="min-w-0 flex-1">
                <span className="block truncate text-sm font-bold">{property.name}</span>
                <span className="mt-1 block text-xs font-semibold">월 ¥{property.rent.toLocaleString("en-US")}</span>
                <span className="mt-1 block text-[11px] text-[var(--text-secondary)]">
                  초기비용 ¥{property.initialCost.toLocaleString("en-US")}
                </span>
                <span className="block text-[11px] font-bold text-[var(--brand)]">
                  생활 가능 {property.livingMonths}
                </span>
              </span>
              <ChevronRight aria-hidden="true" className="size-5 text-[#98a1aa]" />
            </button>
          ))}
        </div>
      </section>

      <div className="fixed inset-x-0 bottom-[calc(100px+env(safe-area-inset-bottom))] z-20 mx-auto w-full max-w-[430px] px-4">
        <button
          type="button"
          onClick={() => void navigate("/properties/new")}
          className="flex h-12 w-full items-center justify-center gap-1 rounded-lg bg-[var(--brand)] text-sm font-bold text-white shadow-sm"
        >
          <Plus aria-hidden="true" className="size-4" />
          매물 등록하기
        </button>
      </div>

      <BottomNav />
    </main>
  )
}

export { PropertiesPage }