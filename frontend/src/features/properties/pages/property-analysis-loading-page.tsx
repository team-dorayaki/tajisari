import { useEffect } from "react"
import { ArrowLeft } from "lucide-react"
import { useNavigate } from "react-router-dom"

import aiLoadingUrl from "@/features/properties/assets/ai_loading.png"

function PropertyAnalysisLoadingPage() {
  const navigate = useNavigate()

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void navigate("/properties/costs", { replace: true })
    }, 5000)

    return () => window.clearTimeout(timer)
  }, [navigate])

  return (
    <main className="flex min-h-dvh flex-col bg-[#f5f6f7]">
      <header className="grid h-[calc(64px+env(safe-area-inset-top))] grid-cols-[44px_1fr_44px] items-end bg-white px-2 pb-3 pt-[env(safe-area-inset-top)]">
        <button
          type="button"
          onClick={() => void navigate(-1)}
          className="grid size-11 place-items-center rounded-full"
          aria-label="뒤로 가기"
        >
          <ArrowLeft aria-hidden="true" className="size-5" />
        </button>
        <h1 className="self-center text-center text-sm font-bold">AI 비용 추출</h1>
      </header>

      <section className="flex flex-1 flex-col items-center px-6 pt-[22vh] text-center">
        <div
          role="img"
          aria-label="매물 정보를 살펴보는 타지살이 캐릭터"
          className="property-ai-loading-sprite"
        >
          {[0, 1, 2, 3, 4].map((frame) => (
            <span
              key={frame}
              aria-hidden="true"
              className="property-ai-loading-frame"
              style={{ backgroundImage: `url(${aiLoadingUrl})` }}
            />
          ))}
        </div>
        <h2 className="text-[20px] font-bold tracking-[-0.025em]">매물 비용을 추출하고 있어요</h2>
        <p className="mt-3 text-xs leading-5 text-[var(--text-secondary)]">
          등록한 정보에서 월세와 초기비용을 찾고 있어요.<br />추출이 끝나면 비용 확인 화면으로 이동해요.
        </p>
        <div className="mt-7 flex gap-2" aria-label="추출 중">
          <span className="size-2 animate-pulse rounded-full bg-[var(--brand)]" />
          <span className="size-2 animate-pulse rounded-full bg-[var(--brand)]/45 [animation-delay:150ms]" />
          <span className="size-2 animate-pulse rounded-full bg-[var(--brand)]/25 [animation-delay:300ms]" />
        </div>
        <p className="mt-5 text-[11px] text-[#a3aab2]">잠시만 기다려주세요</p>
      </section>
    </main>
  )
}

export { PropertyAnalysisLoadingPage }
