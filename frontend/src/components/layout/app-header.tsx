import logoUrl from "@/assets/logo.svg"

function AppHeader() {
  return (
    <header className="flex h-[calc(80px+env(safe-area-inset-top))] shrink-0 items-center gap-2.5 bg-white px-4 pt-[env(safe-area-inset-top)]">
      <img
        src={logoUrl}
        alt=""
        aria-hidden="true"
        className="size-9 shrink-0 object-contain"
      />
      <span className="text-[26px] leading-none font-extrabold tracking-[-0.065em]">
        타지살이
      </span>
    </header>
  )
}

export { AppHeader }
