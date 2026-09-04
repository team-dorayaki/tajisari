import { useSearchParams } from "react-router-dom"

import { AppHeader } from "@/components/layout/app-header"
import { BottomNav } from "@/components/layout/bottom-nav"
import { HomeProgressHero } from "@/features/home/components/home-progress-hero"
import { PriorityPropertiesCard } from "@/features/home/components/priority-properties-card"
import { SettlementChecklistCard } from "@/features/home/components/settlement-checklist-card"
import { SettlementProfileCard } from "@/features/home/components/settlement-profile-card"
import { homeStageContents, isHomeStage } from "@/features/home/home-data"

function HomePage() {
  const [searchParams] = useSearchParams()
  const requestedStage = searchParams.get("stage")
  const stage = isHomeStage(requestedStage) ? requestedStage : "1"
  const completed = stage === "done"
  const showProfile = stage !== "1"

  return (
    <main className="flex min-h-dvh flex-col bg-[#f5f6f7]">
      <AppHeader />
      <HomeProgressHero content={homeStageContents[stage]} completed={completed} />

      <div className="space-y-4 px-4 pt-5 pb-[calc(120px+env(safe-area-inset-bottom))]">
        {showProfile && <SettlementProfileCard />}
        {completed && <PriorityPropertiesCard />}
        <SettlementChecklistCard completed={completed} />
      </div>

      <BottomNav />
    </main>
  )
}

export { HomePage }
