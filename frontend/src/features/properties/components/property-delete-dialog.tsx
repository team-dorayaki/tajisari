import { LoaderCircle, TriangleAlert } from "lucide-react"

type PropertyDeleteDialogProps = {
  count: number
  isDeleting: boolean
  error: string | null
  onCancel: () => void
  onConfirm: () => void
}

function PropertyDeleteDialog({ count, isDeleting, error, onCancel, onConfirm }: PropertyDeleteDialogProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 px-5" role="presentation">
      <section
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="property-delete-title"
        aria-describedby="property-delete-description"
        onKeyDown={(event) => {
          if (event.key === "Escape" && !isDeleting) onCancel()
        }}
        className="w-full max-w-sm rounded-2xl bg-white p-5 shadow-2xl"
      >
        <span className="mx-auto grid size-10 place-items-center rounded-full bg-[#fff0ed] text-[#e45f4c]">
          <TriangleAlert aria-hidden="true" className="size-5" />
        </span>
        <h2 id="property-delete-title" className="mt-4 text-center text-lg font-bold">매물을 삭제할까요?</h2>
        <p id="property-delete-description" className="mt-2 text-center text-sm leading-5 text-[var(--text-secondary)]">
          선택한 매물 {count}개가 삭제돼요. 삭제한 매물은 다시 복구할 수 없어요.
        </p>
        {error && <p className="mt-3 text-center text-xs font-semibold text-[#e45f4c]" role="alert">{error}</p>}
        <div className="mt-6 grid grid-cols-2 gap-2">
          <button type="button" autoFocus disabled={isDeleting} onClick={onCancel} className="h-12 rounded-lg border border-[#dce1e4] text-sm font-bold disabled:opacity-50">
            취소
          </button>
          <button type="button" disabled={isDeleting} onClick={onConfirm} className="flex h-12 items-center justify-center gap-2 rounded-lg bg-[#e45f4c] text-sm font-bold text-white disabled:bg-[#efa79c]">
            {isDeleting && <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />}
            {isDeleting ? "삭제 중..." : "삭제하기"}
          </button>
        </div>
      </section>
    </div>
  )
}

export { PropertyDeleteDialog }
