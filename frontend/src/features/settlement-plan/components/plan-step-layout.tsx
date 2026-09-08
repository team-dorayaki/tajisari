import type { ReactNode } from "react"
import { ArrowLeft } from "lucide-react"
import { useNavigate } from "react-router-dom"

import { Button } from "@/components/ui/button"

type PlanStepLayoutProps = {
  children: ReactNode
  currentStep: number
  totalSteps: number
  nextLabel?: string
  nextDisabled?: boolean
  onBack?: () => void
  onNext: () => void
}

function PlanStepLayout({
  children,
  currentStep,
  totalSteps,
  nextLabel = "다음",
  nextDisabled = false,
  onBack,
  onNext,
}: PlanStepLayoutProps) {
  const navigate = useNavigate()
  const progress = `${(currentStep / totalSteps) * 100}%`

  return (
    <main className="flex min-h-dvh flex-col bg-[#f5f6f7] pb-[calc(104px+env(safe-area-inset-bottom))]">
      <header className="h-[calc(72px+env(safe-area-inset-top))] shrink-0 bg-white pt-[env(safe-area-inset-top)]">
        <div className="flex h-14 items-center px-2">
          <button
            type="button"
            onClick={onBack ?? (() => void navigate(-1))}
            className="grid size-11 place-items-center rounded-full text-[#25282d]"
            aria-label="이전 화면으로 이동"
          >
            <ArrowLeft aria-hidden="true" className="size-5" strokeWidth={2} />
          </button>
        </div>

        <div
          className="mx-4 h-1 overflow-hidden rounded-full bg-[#e5e9ec]"
          role="progressbar"
          aria-label="계획 등록 진행률"
          aria-valuemin={1}
          aria-valuemax={totalSteps}
          aria-valuenow={currentStep}
        >
          <div
            className="h-full rounded-full bg-[var(--brand)] transition-[width]"
            style={{ width: progress }}
          />
        </div>
      </header>

      <section className="flex-1 px-4 pt-7">{children}</section>

      <div className="fixed inset-x-0 bottom-0 z-20 mx-auto w-full max-w-[430px] bg-gradient-to-t from-[#f5f6f7] from-70% to-transparent px-4 pt-6 pb-[calc(14px+env(safe-area-inset-bottom))]">
        <Button
          type="button"
          onClick={onNext}
          disabled={nextDisabled}
          className="h-12 w-full rounded-lg bg-[var(--brand)] text-[15px] font-bold text-white hover:bg-[var(--brand)]/90"
        >
          {nextLabel}
        </Button>
      </div>
    </main>
  )
}

export { PlanStepLayout }
