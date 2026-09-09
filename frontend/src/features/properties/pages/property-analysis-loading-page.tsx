import { useEffect, useState } from "react"
import { AlertCircle, ArrowLeft } from "lucide-react"
import { useLocation, useNavigate } from "react-router-dom"

import aiLoadingUrl from "@/features/properties/assets/ai_loading.png"
import { analyzePropertyImages, analyzePropertyUrl } from "@/features/properties/api/property-analysis-api"
import { usePropertyCostsStore } from "@/features/properties/store/property-costs-store"

type AnalysisNavigationState =
  | { kind: "images"; files: File[] }
  | { kind: "url"; url: string }

function PropertyAnalysisLoadingPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const loadAnalysisResult = usePropertyCostsStore((state) => state.loadAnalysisResult)
  const [error, setError] = useState<string | null>(null)
  const request = location.state as AnalysisNavigationState | null

  useEffect(() => {
    if (!request) {
      void navigate("/properties/new", { replace: true })
      return
    }

    let cancelled = false
    const run = async () => {
      try {
        const result = request.kind === "images"
          ? await analyzePropertyImages(request.files)
          : await analyzePropertyUrl(request.url)
        if (cancelled) return
        loadAnalysisResult(result, request.kind === "images" ? request.files : [])
        void navigate("/properties/costs", { replace: true })
      } catch (caught) {
        if (!cancelled) setError(caught instanceof Error ? caught.message : "매물 분석에 실패했어요.")
      }
    }
    void run()
    return () => { cancelled = true }
  }, [loadAnalysisResult, navigate, request])

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
        {error ? (
          <>
            <AlertCircle aria-hidden="true" className="size-10 text-[#ff705d]" />
            <h2 className="mt-5 text-[20px] font-bold tracking-[-0.025em]">매물 분석에 실패했어요</h2>
            <p className="mt-3 text-xs leading-5 text-[var(--text-secondary)]">{error}</p>
            <button type="button" onClick={() => void navigate(-1)} className="mt-7 h-12 rounded-lg bg-[var(--brand)] px-5 text-sm font-bold text-white">다시 등록하기</button>
          </>
        ) : <>
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
        </>}
      </section>
    </main>
  )
}

export { PropertyAnalysisLoadingPage }
