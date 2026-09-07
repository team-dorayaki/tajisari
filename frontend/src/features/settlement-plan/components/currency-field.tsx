type CurrencyFieldProps = {
  label?: string
  value: number
  currency: "KRW" | "JPY"
  onChange: (value: number) => void
}

import { parseAmount } from "@/features/settlement-plan/utils/amount"

function CurrencyField({ label, value, currency, onChange }: CurrencyFieldProps) {
  return (
    <label className="block">
      {label && (
        <span className="mb-2 block text-xs font-semibold text-[var(--text-secondary)]">
          {label}
        </span>
      )}
      <span className="flex min-h-12 items-center gap-3 border-b border-[#d9dfe3] focus-within:border-[var(--brand)]">
        <input
          type="text"
          inputMode="numeric"
          value={value.toLocaleString("ko-KR")}
          onChange={(event) => onChange(parseAmount(event.target.value))}
          className="min-w-0 flex-1 bg-transparent text-[17px] font-bold tabular-nums outline-none"
          aria-label={`${label ?? "금액"} ${currency}`}
        />
        <span className="text-xs font-semibold text-[var(--text-secondary)]">{currency}</span>
      </span>
    </label>
  )
}

export { CurrencyField }
