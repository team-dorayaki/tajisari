import { Building2, House, UserRound } from "lucide-react"
import { NavLink } from "react-router-dom"

import { cn } from "@/lib/utils"

const navItems = [
  { label: "홈", href: "/home", icon: House },
  { label: "매물", href: "/properties", icon: Building2 },
  { label: "마이", href: "/my", icon: UserRound },
]

function BottomNav() {
  return (
    <div className="pointer-events-none fixed inset-x-0 bottom-0 z-30 mx-auto w-full max-w-[430px] bg-gradient-to-t from-[#f5f6f7] from-60% to-transparent px-4 pt-6 pb-[calc(16px+env(safe-area-inset-bottom))]">
      <nav
        aria-label="주요 메뉴"
        className="pointer-events-auto grid h-[68px] grid-cols-3 rounded-full bg-white px-4 shadow-[0_8px_28px_rgb(15_23_42/0.14)]"
      >
        {navItems.map(({ label, href, icon: Icon }) => (
          <NavLink
            key={href}
            to={href}
            className={({ isActive }) =>
              cn(
                "mx-auto flex size-[60px] flex-col items-center justify-center gap-1 rounded-full text-[11px] font-medium text-[#7f8995] transition-colors",
                isActive && "bg-[var(--brand-soft)] text-[var(--brand)]",
              )
            }
          >
            <Icon aria-hidden="true" className="size-5" strokeWidth={2} />
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  )
}

export { BottomNav }
