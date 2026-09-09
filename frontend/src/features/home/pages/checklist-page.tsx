import { ArrowLeft, Plus, Trash2 } from "lucide-react"
import { useNavigate } from "react-router-dom"

import { BottomNav } from "@/components/layout/bottom-nav"
import checklistAssetsUrl from "@/features/home/assets/checklist-assets.png"
import { useChecklistStore } from "@/features/home/store/checklist-store"

function ChecklistPage() {
  const navigate = useNavigate()
  const items = useChecklistStore((state) => state.items)
  const addItem = useChecklistStore((state) => state.addItem)
  const updateItem = useChecklistStore((state) => state.updateItem)
  const removeItem = useChecklistStore((state) => state.removeItem)
  const toggleItem = useChecklistStore((state) => state.toggleItem)

  return (
    <main className="min-h-dvh bg-[#f5f6f7] pb-[calc(120px+env(safe-area-inset-bottom))]">
      <header className="bg-white pt-[env(safe-area-inset-top)]">
        <div className="grid h-14 grid-cols-[44px_1fr_44px] items-center px-2">
          <button type="button" onClick={() => navigate(-1)} className="grid size-11 place-items-center rounded-full" aria-label="뒤로 가기">
            <ArrowLeft aria-hidden="true" className="size-5" />
          </button>
          <h1 className="text-center text-sm font-bold">정착 체크리스트</h1>
        </div>
      </header>

      <section className="px-4 pt-6">
        <h2 className="text-[28px] font-bold tracking-[-0.05em]">나의 정착 체크리스트</h2>
        <p className="mt-2 text-[15px] text-[var(--text-secondary)]">출국 전/후 할 일을 관리해요.</p>

        <div className="mt-6 overflow-hidden rounded-[20px] bg-white">
          <ul>
            {items.map((item) => (
              <li key={item.id} className="flex items-center gap-3 px-4 py-3">
                <button
                  type="button"
                  onClick={() => toggleItem(item.id)}
                  aria-pressed={item.completed}
                  aria-label={`${item.label} ${item.completed ? "완료 취소" : "완료"}`}
                  className="size-8 shrink-0 bg-no-repeat"
                  style={{
                    backgroundImage: `url(${checklistAssetsUrl})`,
                    backgroundPosition: item.completed ? "0 0" : "-32px 0",
                    backgroundSize: "64px 32px",
                  }}
                />
                <input
                  value={item.label}
                  onChange={(event) => updateItem(item.id, event.target.value)}
                  className="min-w-0 flex-1 border-b border-[#dce2e6] bg-transparent py-1 text-sm font-medium outline-none focus:border-[var(--brand)]"
                  aria-label="체크리스트 항목"
                />
                <button type="button" onClick={() => removeItem(item.id)} className="grid size-9 shrink-0 place-items-center rounded-full text-[#ff6f70]" aria-label={`${item.label} 삭제`}>
                  <Trash2 aria-hidden="true" className="size-5" strokeWidth={2.3} />
                </button>
              </li>
            ))}
          </ul>
          <button type="button" onClick={addItem} className="flex min-h-13 w-full items-center justify-center gap-2 border-t border-[#edf0f2] text-sm font-bold text-[var(--brand)]">
            <Plus aria-hidden="true" className="size-4" />
            항목 추가
          </button>
        </div>
      </section>
      <BottomNav />
    </main>
  )
}

export { ChecklistPage }
