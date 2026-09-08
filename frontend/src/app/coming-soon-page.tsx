import { ArrowLeft } from "lucide-react"
import { useNavigate } from "react-router-dom"

function ComingSoonPage() {
  const navigate = useNavigate()

  return (
    <main className="flex min-h-dvh flex-col bg-[#f5f6f7]">
      <header className="flex h-16 items-center bg-white px-4 pt-[env(safe-area-inset-top)]">
        <button
          type="button"
          onClick={() => void navigate(-1)}
          className="grid size-11 place-items-center rounded-full"
          aria-label="뒤로 가기"
        >
          <ArrowLeft aria-hidden="true" className="size-5" />
        </button>
      </header>
      <section className="flex flex-1 flex-col items-center justify-center px-6 text-center">
        <div className="grid size-16 place-items-center rounded-full bg-[var(--brand-soft)] text-3xl" aria-hidden="true">
          🧭
        </div>
        <h1 className="mt-5 text-xl font-bold">다음 기능에서 만나요</h1>
        <p className="mt-2 text-sm text-[var(--text-secondary)]">현재 화면은 다음 이슈에서 구현할 예정이에요.</p>
      </section>
    </main>
  )
}

export { ComingSoonPage }
