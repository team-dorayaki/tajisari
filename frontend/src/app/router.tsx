import { createBrowserRouter } from "react-router-dom"

import { MobileAppShell } from "@/components/layout/mobile-app-shell"
import { HomePage } from "@/features/home/pages/home-page"
import { SplashPage } from "@/features/onboarding/pages/splash-page"
import { StartPage } from "@/features/onboarding/pages/start-page"

const router = createBrowserRouter([
  {
    element: <MobileAppShell />,
    children: [
      { index: true, element: <SplashPage /> },
      { path: "start", element: <StartPage /> },
      { path: "home", element: <HomePage /> },
    ],
  },
])

export { router }
