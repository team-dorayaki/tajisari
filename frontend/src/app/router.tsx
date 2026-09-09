import { createBrowserRouter } from "react-router-dom"

import { MobileAppShell } from "@/components/layout/mobile-app-shell"
import { ChecklistPage } from "@/features/home/pages/checklist-page"
import { HomePage } from "@/features/home/pages/home-page"
import { SplashPage } from "@/features/onboarding/pages/splash-page"
import { StartPage } from "@/features/onboarding/pages/start-page"
import { PropertiesPage } from "@/features/properties/pages/properties-page"
import { PropertyAnalysisLoadingPage } from "@/features/properties/pages/property-analysis-loading-page"
import { PropertyCostsPage, RequiredCostsPage } from "@/features/properties/pages/property-costs-page"
import { PropertyDetailPage } from "@/features/properties/pages/property-detail-page"
import { PropertyComparisonPage } from "@/features/properties/pages/property-comparison-page"
import {
  ImageRegistrationPage,
  PropertyRegistrationPage,
  UrlRegistrationPage,
} from "@/features/properties/pages/property-registration-page"
import { SettlementPlanPage } from "@/features/settlement-plan/pages/settlement-plan-page"
import { MySettlementPlanPage } from "@/features/settlement-plan/pages/my-settlement-plan-page"
import { ExchangeRateEditPage } from "@/features/settlement-plan/pages/exchange-rate-edit-page"

const router = createBrowserRouter([
  {
    element: <MobileAppShell />,
    children: [
      { index: true, element: <SplashPage /> },
      { path: "start", element: <StartPage /> },
      { path: "home", element: <HomePage /> },
      { path: "plan", element: <SettlementPlanPage /> },
      { path: "plan/exchange-rate", element: <ExchangeRateEditPage /> },
      { path: "plan/:step", element: <SettlementPlanPage /> },
      { path: "properties", element: <PropertiesPage /> },
      { path: "properties/new", element: <PropertyRegistrationPage /> },
      { path: "properties/new/image", element: <ImageRegistrationPage /> },
      { path: "properties/new/url", element: <UrlRegistrationPage /> },
      { path: "properties/analyzing", element: <PropertyAnalysisLoadingPage /> },
      { path: "properties/costs", element: <PropertyCostsPage /> },
      { path: "properties/costs/review", element: <RequiredCostsPage /> },
      { path: "properties/:propertyId", element: <PropertyDetailPage /> },
      { path: "properties/compare", element: <PropertyComparisonPage /> },
      { path: "my", element: <MySettlementPlanPage /> },
      { path: "checklist", element: <ChecklistPage /> },
    ],
  },
])

export { router }
