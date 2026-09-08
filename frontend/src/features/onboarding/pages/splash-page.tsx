import { useEffect } from "react"
import { useNavigate } from "react-router-dom"

import { BrandLockup } from "@/features/onboarding/components/brand-lockup"

const SPLASH_DURATION_MS = 1200

function SplashPage() {
  const navigate = useNavigate()

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void navigate("/start", { replace: true })
    }, SPLASH_DURATION_MS)

    return () => window.clearTimeout(timer)
  }, [navigate])

  return (
    <main
      className="flex min-h-dvh items-center justify-center px-6"
      aria-label="타지살이 시작 화면"
    >
      <div className="animate-in fade-in zoom-in-95 duration-500">
        <BrandLockup />
      </div>
    </main>
  )
}

export { SplashPage }
