import { createBrowserRouter } from "react-router-dom"

import { ComingSoonPage } from "@/app/coming-soon-page"
import { MobileAppShell } from "@/components/layout/mobile-app-shell"
import { HomePage } from "@/features/home/pages/home-page"
import { SplashPage } from "@/features/onboarding/pages/splash-page"
import { StartPage } from "@/features/onboarding/pages/start-page"
import { SettlementPlanPage } from "@/features/settlement-plan/pages/settlement-plan-page"

const router = createBrowserRouter([
  {
    element: <MobileAppShell />,
    children: [
      { index: true, element: <SplashPage /> },
      { path: "start", element: <StartPage /> },
      { path: "home", element: <HomePage /> },
      { path: "plan", element: <SettlementPlanPage /> },
      { path: "plan/:step", element: <SettlementPlanPage /> },
      { path: "properties", element: <ComingSoonPage /> },
      { path: "properties/new", element: <ComingSoonPage /> },
      { path: "properties/compare", element: <ComingSoonPage /> },
      { path: "my", element: <ComingSoonPage /> },
      { path: "checklist", element: <ComingSoonPage /> },
    ],
  },
])

export { router }
