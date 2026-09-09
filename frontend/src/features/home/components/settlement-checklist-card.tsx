import { Link } from "react-router-dom"

import checklistAssetsUrl from "@/features/home/assets/checklist-assets.png"
import { useChecklistStore } from "@/features/home/store/checklist-store"

function SettlementChecklistCard() {
  const items = useChecklistStore((state) => state.items)
  const toggleItem = useChecklistStore((state) => state.toggleItem)
  const completedCount = items.filter((item) => item.completed).length
  const visibleItems = items.slice(0, 5)

  return (
    <section className="rounded-[20px] bg-white p-5" aria-labelledby="settlement-checklist-title">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h2 id="settlement-checklist-title" className="text-base font-bold">
            정착 체크리스트
          </h2>
          <span className="text-xs font-medium text-[var(--text-secondary)]">{completedCount}/{items.length} 완료</span>
        </div>
        <Link to="/checklist" className="text-xs font-semibold text-[var(--brand)]">
          전체보기 ›
        </Link>
      </div>

      <ol className="mt-4 flex">
        {visibleItems.map((item) => {
          const isCompleted = item.completed

          return (
            <li key={item.id} className="w-1/5 shrink-0">
              <button
                type="button"
                onClick={() => toggleItem(item.id)}
                aria-pressed={isCompleted}
                aria-label={`${item.label} ${isCompleted ? "완료 취소" : "완료"}`}
                className="flex w-full min-w-0 flex-col items-center gap-2 text-center"
              >
                <span
                  aria-hidden="true"
                  className="h-8 w-10 bg-no-repeat"
                style={{
                  backgroundImage: `url(${checklistAssetsUrl})`,
                  backgroundPosition: isCompleted ? "0 0" : "-40px 0",
                  backgroundSize: "80px 40px",
                }}
                />
                <span className={isCompleted ? "text-[10px] font-semibold text-[var(--brand)]" : "text-[10px] text-[#9da5ad]"}>
                  {item.label}
                </span>
              </button>
            </li>
          )
        })}
      </ol>
    </section>
  )
}

export { SettlementChecklistCard }
