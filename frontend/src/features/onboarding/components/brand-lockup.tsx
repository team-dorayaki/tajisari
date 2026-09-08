import logoUrl from "@/assets/logo.svg"

type BrandLockupProps = {
  compact?: boolean
}

function BrandLockup({ compact = false }: BrandLockupProps) {
  return (
    <div className="flex flex-col items-center" aria-label="타지살이">
      <img
        src={logoUrl}
        alt=""
        className={compact ? "size-20" : "size-24"}
        aria-hidden="true"
      />

      <h1
        className={
          compact
            ? "mt-6 text-[40px] font-extrabold tracking-[-0.06em]"
            : "mt-7 text-[43px] font-extrabold tracking-[-0.06em]"
        }
      >
        타지<span className="text-[var(--brand)]">살이</span>
      </h1>
    </div>
  )
}

export { BrandLockup }
