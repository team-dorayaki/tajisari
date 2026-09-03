import { useNavigate } from "react-router-dom"

import { BrandLockup } from "@/features/onboarding/components/brand-lockup"

function StartPage() {
  const navigate = useNavigate()

  return (
    <main className="flex min-h-dvh flex-col px-6 pb-[calc(24px+env(safe-area-inset-bottom))] pt-[max(24px,env(safe-area-inset-top))]">
      <section className="flex flex-1 flex-col items-center justify-center pb-6 text-center">
        <BrandLockup compact />
        <p className="mt-3 text-sm tracking-[-0.02em] text-[var(--text-secondary)]">
          일본 첫 정착, 내 자금으로 가능한지 확인해보세요
        </p>
      </section>

      <p className="mb-3 text-center text-xs text-[var(--text-secondary)]/70">
        현재는 일본 정착만 지원해요
      </p>
      <button
        type="button"
        className="min-h-14 w-full rounded-xl bg-[#17191f] px-5 text-base font-bold text-white transition active:translate-y-px active:bg-black focus-visible:ring-3 focus-visible:ring-[var(--brand)]/30"
        onClick={() => void navigate("/home")}
      >
        타지살이 시작하기
      </button>
    </main>
  )
}

export { StartPage }
