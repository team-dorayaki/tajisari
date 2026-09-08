import { Outlet } from "react-router-dom"

function MobileAppShell() {
  return (
    <div className="mobile-app-shell mx-auto min-h-dvh w-full max-w-[430px] overflow-x-hidden bg-[var(--app-surface)]">
      <Outlet />
    </div>
  )
}

export { MobileAppShell }
