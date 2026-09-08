import type { ReactNode } from "react"

import logoUrl from "@/assets/logo.svg"
import { cn } from "@/lib/utils"

type BrandInsightCardProps = {
  title: ReactNode
  children: ReactNode
  className?: string
}

function BrandInsightCard({ title, children, className }: BrandInsightCardProps) {
  return (
    <aside className={cn("rounded-xl bg-[var(--brand-soft)] px-4 py-4", className)}>
      <div className="flex items-center gap-2.5">
        <img src={logoUrl} alt="" aria-hidden="true" className="size-6 shrink-0 object-contain" />
        <h3 className="text-sm font-bold tracking-[-0.015em]">{title}</h3>
      </div>
      <div className="mt-2.5 text-xs leading-5 text-[var(--text-secondary)]">{children}</div>
    </aside>
  )
}

export { BrandInsightCard }
